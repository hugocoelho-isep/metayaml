package pt.isep.metayaml.inference;

import pt.isep.metayaml.inference.rules.*;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.ParsedDocument;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Orchestrates the metamodel inference process.
 *
 * <p>Implements the creation phase of the Cánovas Izquierdo & Cabot (2013)
 * algorithm, applying rules C1–C5 recursively over each parsed YAML document.
 *
 * <p>Rules are evaluated in priority order: C1 and C2 (structural rules)
 * before C3 and C4 (scalar rules), with C5 as the fallback.
 */
public class MetamodelInferenceEngine implements IRuleEngine {

    private final List<ICreationRule> creationRules;
    private final List<IRefinementRule> refinementRules;

    // Per-DSL pre-scan: class name -> distinct child keys seen inside mapping-of-mappings.
    private final Map<String, Set<String>> childKeysByClass = new HashMap<>();

    public MetamodelInferenceEngine() {
        this.creationRules = List.of(
                new C1_MappingListRule(),
                new C2_MappingRule(),
                new C3_ScalarListRule(),
                new C4_ScalarRule(),
                new C5_MixedListRule()
        ); // The list of order must be on this order

       this.refinementRules = List.of(
               new R1_PolymorphicFeatureRule(),    // scalar+object conflicts -> abstract + 2 subtypes
               new R2_OptionalRule(),             // mark optional by occurrence count
               new R3_SharedClassOptionalRule(),   // shared classes are always optional
               new R4_TypeRefinementRule(),        // refine NULL types
               new R5_EmptyClassRemovalRule(),      // remove empty artefact classes
               new R6_OpenMapRule(),               // collapse open-map classes to MAP attributes
               new R7_OpenListMapRule(),            // collapse open-list-maps to MatrixParameter + matrix include/exclude to KeyValuePair entries
               new R8_ClassMergeRule(),            // merge structurally equivalent classes
               new R9_PassThroughEliminationRule() // collapse pass-through containers
       );
    }

    /**
     * Infers a metamodel from a list of parsed documents of the same DSL type.
     *
     * @param dslName   the DSL type name (e.g. "github-actions")
     * @param documents parsed YAML documents belonging to this DSL
     * @return the inferred metamodel
     */
    public InferredMetamodel infer(String dslName, List<ParsedDocument> documents) {
        scanContainerCandidates(documents);
        InferredMetamodel metamodel = new InferredMetamodel(dslName);
        
        for(ParsedDocument doc : documents) {
            String rootClassName = deriveRootClassName(dslName);
            MetaClass rootClass = metamodel.getOrCreateClass(rootClassName);
            rootClass.incrementOccurrences();
            processMapping(doc.content(), rootClass, metamodel);
        }

        for(IRefinementRule rule: refinementRules){
            rule.apply(metamodel);
        }

        return metamodel;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void processMapping(Map<String, Object> mapping, MetaClass owner, InferredMetamodel metamodel) {
        // Cast through Object to bypass generic type enforcement; some YAML maps have non-String keys
        // (e.g. complex/flow keys parsed by SnakeYAML as LinkedHashMap) — those are skipped.
        Map<Object, Object> raw = (Map<Object, Object>) (Object) mapping;
        for (Map.Entry<Object, Object> entry : raw.entrySet()) {
            if (entry.getKey() instanceof String key) {
                applyCreationRule(key, entry.getValue(), owner, metamodel);
            }
        }
    }

    private void applyCreationRule(String key, Object value, MetaClass owner, InferredMetamodel metamodel) {
        String normalizedKey = key.replace('-', '_');
        for(ICreationRule rule: creationRules){
            if(rule.appliesTo(value)){
                rule.apply(normalizedKey, value, owner, metamodel, this);
                return;
            }
        }
    }

    @Override
    public int distinctChildKeys(String className) {
        Set<String> keys = childKeysByClass.get(className);
        return keys == null ? 0 : keys.size();
    }

    /**
     * Pre-scan over all documents of the current DSL: for every mapping whose entries are
     * all sub-mappings (a candidate named-map container), record the distinct child keys
     * under the class name its key would produce ({@code capitalize(normalizedKey)}). The
     * counts let {@link C2_MappingRule} tell an open key space (many distinct instance
     * names) from a fixed block (one or two recurring keys) without hardcoding class names.
     */
    private void scanContainerCandidates(List<ParsedDocument> documents) {
        childKeysByClass.clear();
        for (ParsedDocument doc : documents) {
            scanMapping(doc.content());
        }
    }

    private void scanMapping(Map<?, ?> node) {
        for (Map.Entry<?, ?> entry : node.entrySet()) {
            if (!(entry.getKey() instanceof String key)) continue;
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> map && !map.isEmpty()) {
                long mapChildren = map.values().stream().filter(v -> v instanceof Map).count();
                if (mapChildren == map.size()) { // all entries are sub-mappings
                    String className = capitalize(key.replace('-', '_'));
                    Set<String> keys = childKeysByClass.computeIfAbsent(className, k -> new HashSet<>());
                    for (Object childKey : map.keySet()) {
                        if (childKey instanceof String s) keys.add(s);
                    }
                }
                scanMapping(map);
            } else if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) scanMapping(itemMap);
                }
            }
        }
    }

    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Derives a root class name from the DSL name.
     * e.g. "github-actions" → "GithubActions", "docker-compose" → "DockerCompose"
     */
    private String deriveRootClassName(String dslName) {
        String[] parts = dslName.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if(!part.isEmpty()){
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
