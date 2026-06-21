package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;

/**
 * R1 — Optional rule.
 *
 * <p>A feature (attribute or reference) is mandatory only if it was seen in
 * every occurrence of its owner class. If a class was encountered N times but
 * a feature appeared fewer than N times, the feature is marked optional.
 *
 * <p>Example — {@code Step} in GitHub Actions is seen 10 times. {@code with}
 * was seen only 1 time → {@code with} becomes {@code [0..1]}.
 */
public class R1_OptionalRule implements IRefinementRule{
    @Override
    public void apply(InferredMetamodel metamodel) {
        for (MetaClass metaClass: metamodel.getClasses()){
            int classOccurrences = metaClass.getOccurrences();

            metaClass.getAttributes().forEach(attr -> {
                if(attr.getOccurrences() < classOccurrences){
                    attr.setOptional(true);
                }
            });

            metaClass.getReferences().forEach(ref -> {
                if(ref.getOccurrences() < classOccurrences){
                    ref.setOptional(true);
                }
            });
        }
    }
}
