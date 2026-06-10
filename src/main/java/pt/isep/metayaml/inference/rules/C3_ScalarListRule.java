package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;

import java.util.List;

/**
 * C3 — Scalar list rule.
 *
 * <p>When a YAML key maps to a list whose elements are all scalars,
 * a {@link MetaAttribute} with {@code many=true} is added to the owner class.
 *
 * <p>Example:
 * <pre>
 *   branches: [main, develop]  → MetaAttribute(name="branches", type=STRING, many=true)
 *   ports: [8080, 9090]        → MetaAttribute(name="ports",    type=INTEGER, many=true)
 * </pre>
 */
public class C3_ScalarListRule implements ICreationRule{

    @Override
    public boolean appliesTo(Object value) {
        if(!(value instanceof List<?> list) || list.isEmpty())
            return false;

        return list.stream().allMatch(C2_ScalarRule::isScalar);
    }

    @Override
    public void apply(String key, Object value, MetaClass owner, InferredMetamodel metamodel, IRuleEngine engine) {
        List<?> list = (List<?>) value;

        DataType type = list.stream()
                .map(C2_ScalarRule::inferType)
                .reduce(C3_ScalarListRule::mergeTypes)
                .orElse(DataType.UNKNOWN);

        owner.findAttribute(key).ifPresentOrElse(
                existing -> {
                    existing.incrementOccurrences();
                    existing.setMany(true);
                },
                () -> owner.addAttribute(new MetaAttribute(key, type, false, true))
        );
    }

    /**
     * If a list contains mixed types, widen to STRING (most general scalar type).
     */
    private static DataType mergeTypes(DataType a, DataType b) {
        return a == b ? a : DataType.STRING;
    }
}
