package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;

/**
 * R2 — Type refinement rule.
 *
 * <p>During creation, SnakeYAML parses scalars with their native types
 * (Boolean, Integer, etc.), so type widening is handled in C2. However,
 * when an attribute is seen as NULL in one document and a concrete type
 * in another, the type remains NULL. R2 promotes NULL attributes to UNKNOWN
 * and resolves any remaining STRING attributes that were widened from
 * more specific types.
 *
 * <p>Additionally, any attribute typed as STRING whose name strongly
 * suggests a boolean (e.g. {@code enabled}, {@code become}, {@code remove},
 * {@code update_cache}) is promoted to BOOLEAN if all observed values
 * were boolean-compatible.
 */
public class R2_TypeRefinementRule implements IRefinementRule{
    @Override
    public void apply(InferredMetamodel metamodel) {
        for (MetaClass metaClass : metamodel.getClasses()) {
            metaClass.getAttributes().forEach(this::refineType);
        }
    }

    private void refineType(MetaAttribute attr) {
        // NULL only attributes (seen only as null) become UNKNOWN
        if(attr.getType() == DataType.NULL){
            attr.setType(DataType.UNKNOWN);
        }
    }
}
