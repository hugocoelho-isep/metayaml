package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.ArrayList;
import java.util.List;

/**
 * R — Polymorphic (scalar-vs-object) feature rule.
 *
 * <p>The same YAML key can appear as a scalar in some documents and as a nested
 * mapping in others. The creation rules (C1, C2/C3) accumulate features
 * independently, leaving a class with both a scalar attribute and a containment
 * reference sharing the same name.
 *
 * <p>Rather than discarding the scalar form (which would make a perfectly valid
 * document non-conformant), this rule generalises the feature into an abstract
 * supertype with two concrete subtypes — one for each shape:
 * <pre>
 *   before: Job.environment : STRING            (from  environment: production)
 *           Job --[environment]--> Environment  (from  environment: {name, url})
 *
 *   after:  abstract Environment
 *           EnvironmentObject extends Environment { name, url }
 *           EnvironmentValue  extends Environment { value }
 *           Job --[environment]--> Environment   (abstract; accepts either shape)
 * </pre>
 *
 * <p>The existing reference target becomes the abstract supertype: its own
 * features move into a {@code <Name>Object} subtype, and a {@code <Name>Value}
 * subtype is created to carry the scalar shape as a single {@code value}
 * attribute. The scalar attribute is then removed from the owner, its
 * occurrences folded into the reference so {@link R1_OptionalRule} still derives
 * the correct optionality.
 *
 * <p>This rule replaces the older lossy {@code R_FeatureConflictRule} and must
 * run first, before the optionality / merge / cleanup rules.
 */
public class R_PolymorphicFeatureRule implements IRefinementRule {

    @Override
    public void apply(InferredMetamodel metamodel) {
        List<MetaClass> snapshot = new ArrayList<>(metamodel.getClasses());
        List<MetaClass> newClasses = new ArrayList<>();

        for (MetaClass owner : snapshot) {
            List<String> conflicts = owner.getAttributes().stream()
                    .map(MetaAttribute::getName)
                    .filter(name -> owner.findReference(name).isPresent())
                    .toList();

            for (String name : conflicts) {
                MetaAttribute attr = owner.findAttribute(name).orElseThrow();
                MetaReference ref = owner.findReference(name).orElseThrow();
                MetaClass supertype = ref.getTarget(); // becomes the abstract supertype

                // The same object class may back this feature on several owners; once the
                // hierarchy exists, just fold the stray scalar into the (abstract) reference.
                if (supertype.isAbstract()) {
                    for (int i = 0; i < attr.getOccurrences(); i++) ref.incrementOccurrences();
                    owner.removeAttribute(name);
                    continue;
                }

                // If the object form has no structure of its own it is not a real
                // scalar-vs-object union — just a scalar (the object was empty / a parse
                // artefact). Leave the conflict for R_EmptyClassRemovalRule, which drops the
                // empty class and keeps the existing scalar attribute.
                if (supertype.getAttributes().isEmpty() && supertype.getReferences().isEmpty()) {
                    continue;
                }

                // Fold the scalar attribute's evidence into the reference and drop it,
                // so optionality (R1) is computed from the combined occurrences.
                for (int i = 0; i < attr.getOccurrences(); i++) ref.incrementOccurrences();
                owner.removeAttribute(name);

                // <Name>Object subtype — receives the object shape's own features.
                MetaClass objectSub = new MetaClass(supertype.getName() + "Object");
                for (MetaAttribute a : new ArrayList<>(supertype.getAttributes())) {
                    objectSub.addAttribute(a);
                    supertype.removeAttribute(a.getName());
                }
                for (MetaReference r : new ArrayList<>(supertype.getReferences())) {
                    objectSub.addReference(r);
                    supertype.removeReference(r.getName());
                }
                for (int i = 0; i < supertype.getOccurrences(); i++) objectSub.incrementOccurrences();
                objectSub.setSuperType(supertype);

                // <Name>Value subtype — carries the scalar shape as a single value. The
                // scalar shape always carries its value, so keep the attribute's occurrences
                // in lock-step with the subtype's, ensuring R1 keeps it mandatory.
                MetaClass valueSub = new MetaClass(supertype.getName() + "Value");
                MetaAttribute valueAttr = new MetaAttribute("value", attr.getType(), false, attr.isMany());
                valueSub.addAttribute(valueAttr);
                for (int i = 0; i < attr.getOccurrences(); i++) valueSub.incrementOccurrences();
                for (int i = 1; i < attr.getOccurrences(); i++) valueAttr.incrementOccurrences();
                valueSub.setSuperType(supertype);

                supertype.setAbstract(true);
                newClasses.add(objectSub);
                newClasses.add(valueSub);

                System.out.printf("[INFO] R_Polymorphic: '%s.%s' is scalar+object -> abstract '%s' { %s, %s }%n",
                        owner.getName(), name, supertype.getName(), objectSub.getName(), valueSub.getName());
            }
        }

        for (MetaClass c : newClasses) {
            metamodel.addClass(c);
        }
    }
}
