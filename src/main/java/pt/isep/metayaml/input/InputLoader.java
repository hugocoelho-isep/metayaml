package pt.isep.metayaml.input;

import pt.isep.metayaml.config.DslSource;
import pt.isep.metayaml.model.ParsedDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InputLoader {
    private final YamlFileDiscovery discovery;
    private final YamlParser parser;

    public InputLoader() {
        this.discovery = new YamlFileDiscovery();
        this.parser    = new YamlParser();
    }

    /**
     * Loads all parseable YAML documents from the given DSL sources.
     *
     * <p>Uses a two-pass approach:
     * <ol>
     *   <li>Parse all files into mutable maps (with null→empty-map normalization).
     *   <li>Collect all keys that appear as nested mappings in at least one document.
     *   <li>For those keys, convert string-list values (e.g. {@code on: [push, pull_request]})
     *       into map-of-empty-maps (e.g. {@code on: {push: {}, pull_request: {}}}), so the
     *       inference engine creates proper class occurrences instead of scalar-list attributes.
     * </ol>
     *
     * @param sources the configured DSL sources
     * @return all successfully parsed, normalised documents
     * @throws IOException if a source directory cannot be listed
     */
    public List<ParsedDocument> loadAll(List<DslSource> sources) throws IOException {
        record RawDoc(String dslName, Path path, Map<String, Object> content) {}
        List<RawDoc> rawDocs = new ArrayList<>();

        // Pass 1: parse all files to mutable maps
        for (DslSource source : sources) {
            List<Path> files = discovery.discover(source.directory());
            if (files.isEmpty()) {
                System.out.printf("[WARN] No YAML files found in '%s' (%s)%n",
                        source.name(), source.directory());
                continue;
            }
            for (Path file : files) {
                try {
                    List<Map<String, Object>> roots = parser.parseToRootMaps(file);
                    for (Map<String, Object> content : roots) {
                        rawDocs.add(new RawDoc(source.name(), file, content));
                    }
                    if (roots.size() > 1) {
                        System.out.printf("[INFO] Parsed: [%s] %s (%d documents)%n",
                                source.name(), file.getFileName(), roots.size());
                    } else {
                        System.out.printf("[INFO] Parsed: [%s] %s%n", source.name(), file.getFileName());
                    }
                } catch (YamlParseException e) {
                    System.out.printf("[WARN] Skipped '%s': %s%n", file.getFileName(), e.getMessage());
                }
            }
        }

        // Collect keys whose values are mappings, scoped PER SOURCE so that each DSL
        // is inferred independently: a key that is a mapping in one DSL must not
        // influence the normalisation of an unrelated DSL configured in the same run.
        Map<String, Set<String>> mappingKeysBySource = new HashMap<>();
        for (RawDoc rd : rawDocs) {
            Set<String> keys = mappingKeysBySource.computeIfAbsent(rd.dslName(), k -> new HashSet<>());
            collectMappingKeys(rd.content(), keys);
        }

        // Pass 2: normalise string-lists → map-of-empty-maps using the per-source
        // mapping keys, then wrap in immutable ParsedDocument
        List<ParsedDocument> documents = new ArrayList<>();
        for (RawDoc rd : rawDocs) {
            normalizeStringLists(rd.content(), mappingKeysBySource.get(rd.dslName()));
            documents.add(new ParsedDocument(rd.dslName(), rd.path(), rd.content()));
        }

        return documents;
    }

    /**
     * Recursively collects all YAML key names (String keys only) whose value
     * is a nested mapping in at least one node of the tree. These keys are
     * candidates for string-list normalisation in other documents.
     *
     * <p>SnakeYAML may produce maps with non-String keys; those are skipped.
     */
    private void collectMappingKeys(Object node, Set<String> mappingKeys) {
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof Map) {
                    mappingKeys.add(key);
                }
                collectMappingKeys(entry.getValue(), mappingKeys);
            }
        } else if (node instanceof List<?> list) {
            for (Object item : list) {
                collectMappingKeys(item, mappingKeys);
            }
        }
    }

    /**
     * Recursively normalises string-list values to map-of-empty-maps for any
     * key that appears as a mapping in at least one other document. This ensures
     * that shorthand forms such as {@code on: [pull_request_target, issues]} are
     * treated as if they were written as the equivalent mapping form
     * {@code on: {pull_request_target: {}, issues: {}}}.
     *
     * <p>Non-String map keys are left untouched.
     */
    @SuppressWarnings("unchecked")
    private void normalizeStringLists(Object node, Set<String> mappingKeys) {
        if (!(node instanceof Map<?, ?> rawMap)) return;
        Map<Object, Object> mutableMap = (Map<Object, Object>) rawMap;

        List<Object> keysToConvert = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : mutableMap.entrySet()) {
            if (entry.getKey() instanceof String key
                    && mappingKeys.contains(key)
                    && entry.getValue() instanceof List<?> list
                    && isAllStrings(list)) {
                keysToConvert.add(key);
            }
        }

        for (Object keyObj : keysToConvert) {
            List<?> list = (List<?>) mutableMap.get(keyObj);
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Object item : list) {
                converted.put((String) item, new LinkedHashMap<>());
            }
            mutableMap.put(keyObj, converted);
        }

        // Recurse into all values (including newly converted maps)
        for (Object val : mutableMap.values()) {
            normalizeStringLists(val, mappingKeys);
        }
    }

    private boolean isAllStrings(List<?> list) {
        return !list.isEmpty() && list.stream().allMatch(item -> item instanceof String);
    }
}
