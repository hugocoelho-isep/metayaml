package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.Map;

/**
 * C2 — Mapping rule.
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
public class C2_MappingRule implements ICreationRule {

    /**
     * Minimum number of distinct child keys (across all documents) for a plural-named
     * class to count as a named-map container. An open key space of user-defined instance
     * names (jobs, services, secrets, …) accumulates many distinct keys; a fixed block
     * keeps one or two recurring ones ({@code defaults → run},
     * {@code resources → limits, reservations}) and is therefore treated as a flat object.
     */
    private static final int CONTAINER_KEY_THRESHOLD = 3;

    @Override
    public boolean appliesTo(Object value) {
        return value instanceof Map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(String key, Object value, MetaClass owner, InferredMetamodel metamodel, IRuleEngine engine) {
        Map<String, Object> mapping = (Map<String, Object>) value;

        String targetName;
        String refName;
        boolean many;

        boolean namedMapContainer = isNamedMapContainer(owner, engine);
        if (namedMapContainer) {
            // e.g. Jobs.build / Jobs.lint / Jobs.release → all become Job
            String singularOwner = singular(owner.getName());
            targetName = capitalize(singularOwner);
            refName    = singularOwner.substring(0, 1).toLowerCase() + singularOwner.substring(1);
            many       = true;
        } else {
            targetName = capitalize(key);
            refName    = key;
            many       = false;
        }

        MetaClass target = metamodel.getOrCreateClass(targetName);
        target.incrementOccurrences();

        if (namedMapContainer) {
            target.findAttribute("id").ifPresentOrElse(
                    MetaAttribute::incrementOccurrences,
                    () -> target.addAttribute(new MetaAttribute("id", DataType.STRING, false, false))
            );
        }

        owner.findReference(refName).ifPresentOrElse(
                existing -> existing.incrementOccurrences(),
                () -> owner.addReference(new MetaReference(refName, target, true, false, many))
        );

        engine.processMapping(mapping, target, metamodel);
    }

    /**
     * Returns true when the owner is a plural container whose children are
     * named instances of a common type (e.g. {@code Jobs}, {@code Services}).
     *
     * <p>Four conditions must all hold:
     * <ol>
     *   <li>The owner has no scalar attributes — its entries are all sub-mappings.</li>
     *   <li>Singularising the owner name produces a different string — it is plural.</li>
     *   <li>The owner name has no internal uppercase letters — it is a simple word,
     *       not a compound like {@code GithubActions}.</li>
     *   <li>Across the corpus it has at least {@value #CONTAINER_KEY_THRESHOLD} distinct
     *       child keys — an open space of user-defined instance names, not a fixed block
     *       like {@code defaults → run} or {@code resources → limits/reservations}.</li>
     * </ol>
     *
     * <p>The last condition replaced a hardcoded {@code NON_CONTAINER_CLASS_NAMES} set
     * (it listed {@code Defaults}); it is inferred from {@link IRuleEngine#distinctChildKeys}.
     */
    private boolean isNamedMapContainer(MetaClass owner, IRuleEngine engine) {
        if (!owner.getAttributes().isEmpty()) return false;
        String name = owner.getName();
        // reject compound names like GithubActions (uppercase after first char)
        for (int i = 1; i < name.length(); i++) {
            if (Character.isUpperCase(name.charAt(i))) return false;
        }
        if (singular(name).equals(name)) return false; // not plural
        return engine.distinctChildKeys(name) >= CONTAINER_KEY_THRESHOLD;
    }

    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String singular(String name) {
        if (name.endsWith("ies"))
            return name.substring(0, name.length() - 3) + "y";
        if (name.endsWith("s") && name.length() > 1)
            return name.substring(0, name.length() - 1);
        return name;
    }
}
