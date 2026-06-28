package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.ArrayList;

/**
 * R9 — Pass-through elimination rule.
 *
 * <p>A "pass-through" class is a <em>synthetic</em> container created by
 * {@link C2_MappingRule}'s named-map-container logic: it has no attributes and a
 * single many-containment reference whose name is the singular of its own (plural)
 * name — {@code Jobs --[job]--> Job}, {@code Services --[service]--> Service}.
 * These intermediate classes exist only to hold instances and add no information.
 *
 * <p>This rule collapses them by rewiring every parent reference that pointed to
 * the pass-through directly to the child, preserving the parent's reference name
 * and setting {@code many=true}.
 *
 * <p>Example:
 * <pre>
 *   GithubActions --[jobs]--> Jobs --[job]--> Job [1..*]
 *   becomes:
 *   GithubActions --[jobs]--> Job [1..*]
 * </pre>
 *
 * <p>The singular-name test is what keeps real semantic wrappers — whose child
 * reference is unrelated to the class name — intact: {@code Workflow_dispatch
 * --[inputs]-->}, {@code Deploy --[resources]-->}, {@code Ipam --[config]-->} are
 * not synthetic and are preserved. This replaced a hardcoded {@code PROTECTED_NAMES}
 * set (GitHub-Actions-specific class names).
 */
public class R9_PassThroughEliminationRule implements IRefinementRule {

    @Override
    public void apply(InferredMetamodel metamodel) {
        boolean changed = true;
        while (changed) {
            changed = tryEliminate(metamodel);
        }
    }

    private boolean tryEliminate(InferredMetamodel metamodel) {
        for (MetaClass candidate : new ArrayList<>(metamodel.getClasses())) {
            if (!isPassThrough(candidate)) continue;

            MetaReference inner = candidate.getReferences().get(0);
            MetaClass child = inner.getTarget();

            for (MetaClass cls : metamodel.getClasses()) {
                for (MetaReference ref : cls.getReferences()) {
                    if (ref.getTarget() == candidate) {
                        ref.setTarget(child);
                        ref.setMany(true);
                        if (inner.isOptional()) ref.setOptional(true);
                    }
                }
            }

            metamodel.removeClass(candidate);
            System.out.printf("[INFO] R9: eliminated pass-through '%s' -> '%s'%n",
                    candidate.getName(), child.getName());
            return true;
        }
        return false;
    }

    private boolean isPassThrough(MetaClass cls) {
        if (cls.isAbstract() || cls.getSuperType() != null) return false;   // keep union super/subtypes
        if (!cls.getAttributes().isEmpty() || cls.getReferences().size() != 1) return false;
        MetaReference ref = cls.getReferences().get(0);
        if (!ref.isMany() || !ref.isContainment()) return false;
        // Synthetic container: sole reference is named after the singular of the (plural) class.
        return ref.getName().equalsIgnoreCase(singular(cls.getName()));
    }

    private String singular(String name) {
        if (name.endsWith("ies")) return name.substring(0, name.length() - 3) + "y";
        if (name.endsWith("s") && name.length() > 1) return name.substring(0, name.length() - 1);
        return name;
    }
}