package pt.isep.metayaml.inference.rules;


import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;

import java.util.*;


/**
 * R3 — Class merge rule.
 *
 * <p>Two classes are candidates for merging if they share the same set of
 * attribute names and reference names (structural equivalence). When merged,
 * all references pointing to the absorbed class are redirected to the survivor,
 * and the absorbed class is removed from the metamodel.
 *
 * <p>Example — in GitHub Actions, {@code Build}, {@code Lint} and {@code Release}
 * all have {@code runs-on} and {@code steps}. They represent the same concept
 * and are merged into the first one encountered.
 *
 * <p>Features present in only one of the merged classes are marked optional.
 */
public class R3_ClassMergeRule implements IRefinementRule {
    @Override
    public void apply(InferredMetamodel metamodel) {
        boolean merged = true;
        while (merged) {
            merged = tryMerge(metamodel);
        }
    }

    private boolean tryMerge(InferredMetamodel metamodel) {
        List<MetaClass> classes = new ArrayList<>(metamodel.getClasses());

        for(int i = 0; i < classes.size(); i++){
            for(int j = i + 1; j < classes.size(); j++){
                MetaClass metaClassA = classes.get(i);
                MetaClass metaClassB = classes.get(j);

                if (arcMergeable(metaClassA, metaClassB)){
                    merge(metaClassA, metaClassB, metamodel);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean arcMergeable(MetaClass metaClassA, MetaClass metaClassB){
        Set<String> attrsA = attributeNames(metaClassA);
        Set<String> attrsB = attributeNames(metaClassB);
        Set<String> refA = referenceNames(metaClassA);
        Set<String> refB = referenceNames(metaClassB);

        // both must have at least one feature
        if(attrsA.isEmpty() && refA.isEmpty())
            return false;
        if(attrsB.isEmpty() && refB.isEmpty())
            return false;

        return attrsA.equals(attrsB) && refA.equals(refB);
    }

    private void merge(MetaClass survivor, MetaClass absorbed, InferredMetamodel metamodel){
        // feature in survivor but not absorved -> optional
        Set<String> absorbedAttrNames = attributeNames(absorbed);
        survivor.getAttributes().forEach(attr -> {
            if(!absorbedAttrNames.contains(attr.getName())){
                attr.setOptional(true);
            }
        });

        // feature in absorbed but not in survivor -> add as optional
        absorbed.getAttributes().forEach(attr -> {
            if(survivor.findAttribute(attr.getName()).isEmpty()){
                attr.setOptional(true);
                survivor.addAttribute(attr);
            }
        });

        // reference in survivor but not in absorbed -> optional
        Set<String> absorbedRefNames = referenceNames(absorbed);
        survivor.getReferences().forEach(ref -> {
            if(!absorbedRefNames.contains(ref.getName())){
                ref.setOptional(true);
            }
        });

        // reference in absorbed but not in survivor -> add as optional
        absorbed.getReferences().forEach(ref -> {
            if(survivor.findReference(ref.getName()).isEmpty()){
                ref.setOptional(true);
                survivor.addReference(ref);
            }
        });

        // redirect all references pointing to absorbed -> survivor
        metamodel.getClasses().forEach(cls->
                cls.getReferences().forEach(ref->{
                    if(ref.getTarget() == absorbed){
                        ref.setTarget(survivor);
                    }
                })
        );

        metamodel.removeClass(absorbed);

        System.out.printf("[INFO] R3: merged '%s' into '%s'%n", absorbed.getName(), survivor.getName());
    }

    private Set<String> attributeNames(MetaClass metaClass){
        Set<String> names = new LinkedHashSet<>();
        metaClass.getAttributes().forEach(a -> names.add(a.getName()));
        return names;
    }

    private Set<String> referenceNames(MetaClass metaClass){
        Set<String> names = new LinkedHashSet<>();
        metaClass.getReferences().forEach(r -> names.add(r.getName()));
        return names;
    }
}
