package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.List;
import java.util.Map;

/**
 * C4 — Mapping list rule.
 *
 * <p>When a YAML key maps to a list whose elements are mappings (objects),
 * a new {@link MetaClass} is created and a containment {@link MetaReference}
 * with {@code many=true} is added to the owner. Each element is processed
 * recursively to populate the target class.
 *
 * <p>Example:
 * <pre>
 *   steps:                    → MetaClass("Step")
 *     - name: Checkout code     owner(Job) --[steps]--> Step [0..*]
 *       uses: actions/checkout@v4
 * </pre>
 */
public class C4_MappingListRule implements ICreationRule{
    @Override
    public boolean appliesTo(Object value) {
        if(!(value instanceof List<?> list) || list.isEmpty())
            return false;

        return list.stream().anyMatch(e -> e instanceof Map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void apply(String key, Object value, MetaClass owner, InferredMetamodel metamodel, IRuleEngine engine) {
        List<?> list = (List<?>) value;
        String targetName = capitalize(singular(key));

        MetaClass target = metamodel.getOrCreateClass(targetName);

        owner.findReference(key).ifPresentOrElse(
                existing -> existing.incrementOccurrences(),
                () -> owner.addReference(new MetaReference(key, target, true, false, true))
        );

        // process each mapping element to populate the target class
        for(Object element : list) {
            if(element instanceof Map<?,?> mapping){
                target.incrementOccurrences();
                engine.processMapping((Map<String, Object>) mapping, target, metamodel);
            }
        }
    }

    /**
     * Naive singularisation: removes trailing 's' if present.
     * e.g. "steps" → "Step", "services" → "Service"
     */
    private String singular(String name){
        if(name.endsWith("ies"))
            return name.substring(0, name.length()-3) + "y";

        if(name.endsWith("s") && name.length()>1)
            return name.substring(0, name.length()-1);

        return name;
    }


    private String capitalize(String name){
        if(name == null || name.isEmpty())
            return name;

        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
