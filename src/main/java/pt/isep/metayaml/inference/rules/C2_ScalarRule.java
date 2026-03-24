package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;

import java.util.Set;

/**
 * C2 — Scalar rule.
 *
 * <p>When a YAML key maps to a scalar value (string, integer, float, boolean,
 * or null), a {@link MetaAttribute} is added to the owner class.
 *
 * <p>Example:
 * <pre>
 *   image: postgres:16    → MetaAttribute(name="image", type=STRING)
 *   replicas: 3           → MetaAttribute(name="replicas", type=INTEGER)
 *   enabled: true         → MetaAttribute(name="enabled", type=BOOLEAN)
 * </pre>
 */
public class C2_ScalarRule implements ICreationRule {

    private static final Set<String> BOOLEAN_STRINGS = Set.of("true", "false", "yes", "no");

    @Override
    public boolean appliesTo(Object value) {
        return isScalar(value);
    }

    @Override
    public void apply(String key, Object value, MetaClass owner, InferredMetamodel metamodel, IRuleEngine engine) {
        DataType type = inferType(value);

        owner.findAttribute(key).ifPresentOrElse(
                existing -> {
                    existing.incrementOccurrences();
                    // widen type if inconsistent across documents
                    if (existing.getType() != type && type != DataType.NULL) {
                        existing.setType(widenType(existing.getType(), type));
                    }
                },
                () -> owner.addAttribute(new MetaAttribute(key, type, false, false))
        );
    }

    static boolean isScalar(Object value) {
        return value == null
                || value instanceof String
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Double
                || value instanceof Float
                || value instanceof Boolean;
    }

    static DataType inferType(Object value) {
        if (value == null)            return DataType.NULL;
        if (value instanceof Boolean) return DataType.BOOLEAN;
        if (value instanceof Integer
                || value instanceof Long)    return DataType.INTEGER;
        if (value instanceof Double
                || value instanceof Float)   return DataType.FLOAT;
        if (value instanceof String s) {
            // SnakeYAML 2.x may return booleans as strings in some contexts
            if (BOOLEAN_STRINGS.contains(s.toLowerCase())) return DataType.BOOLEAN;
        }
        return DataType.STRING;
    }


    /**
     * Widens two incompatible types to a common supertype.
     * Numeric types widen to FLOAT; anything else widens to STRING.
     */
    static DataType widenType(DataType a, DataType b) {
        if (a == b) return a;
        if (isNumeric(a) && isNumeric(b)) return DataType.FLOAT;
        return DataType.STRING;
    }

    private static boolean isNumeric(DataType t) {
        return t == DataType.INTEGER || t == DataType.FLOAT;
    }
}
