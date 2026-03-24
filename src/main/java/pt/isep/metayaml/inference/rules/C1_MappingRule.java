package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.Map;

/**
 * C1 — Mapping rule.
 *
 * <p>When a YAML key maps to a nested mapping (object), a new {@link MetaClass}
 * is created (or reused) and a containment {@link MetaReference} is added to
 * the owner class. The nested mapping is then processed recursively.
 *
 * <p>Example:
 * <pre>
 *   healthcheck:           → MetaClass("Healthcheck")
 *     test: [CMD, ...]       owner(Service) --[healthcheck]--> Healthcheck
 *     interval: 10s
 *     retries: 5
 * </pre>
 */
public class C1_MappingRule implements ICreationRule {

    @Override
    public boolean appliesTo(Object value) {
        return value instanceof Map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(String key, Object value, MetaClass owner, InferredMetamodel metamodel, IRuleEngine engine) {
        Map<String, Object> mapping = (Map<String, Object>) value;
        String targetName = capitalize(key);

        MetaClass target = metamodel.getOrCreateClass(targetName);
        target.incrementOccurrences();

        // add or update reference on the owner
        owner.findReference(key).ifPresentOrElse(
                existing -> existing.setOptional(true),
                () -> owner.addReference(new MetaReference(key, target, false, false, false))
        );

        // recurse into the nested mapping
        engine.processMapping(mapping, target, metamodel);
    }

    private String capitalize(String name) {
        if(name == null || name.isEmpty())
            return name;

        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
