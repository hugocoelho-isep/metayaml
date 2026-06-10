package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;

import java.util.List;

/**
 * C5 — Mixed list rule.
 *
 * <p>Fallback for lists that contain a mix of scalars and mappings,
 * or other unresolvable structures. A {@link MetaAttribute} of type
 * {@code UNKNOWN} with {@code many=true} is added to the owner class.
 *
 * <p>Example:
 * <pre>
 *   test: ["CMD", "pg_isready", "-U", "admin"]
 *   # (list of strings mixed with other types in some configs)
 * </pre>
 */
public class C5_MixedListRule implements ICreationRule {
    @Override
    public boolean appliesTo(Object value) {
        return value instanceof List<?> list && !list.isEmpty();
    }

    @Override
    public void apply(String key, Object value, MetaClass owner, InferredMetamodel metamodel, IRuleEngine engine) {
        owner.findAttribute(key).ifPresentOrElse(
                existing -> existing.incrementOccurrences(),
                () -> owner.addAttribute(new MetaAttribute(key, DataType.UNKNOWN, false, true))
        );
    }
}
