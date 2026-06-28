package pt.isep.metayaml.input;

import pt.isep.metayaml.config.DslSource;
import pt.isep.metayaml.model.ParsedDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * <p>Documents are parsed faithfully: the shapes that actually appear in the
     * YAML (scalars, scalar-lists, mappings, mapping-lists) are preserved as-is.
     * When the same key appears in different shapes across documents — e.g.
     * {@code on: [push, pull_request]} in one file and
     * {@code on: { push: { ... } }} in another — the difference is reconciled later
     * by the inference engine's general polymorphism rule
     * ({@code R1_PolymorphicFeatureRule}), which builds an abstract supertype with a
     * {@code …Value} (the list/scalar shape) and a {@code …Object} (the mapping
     * shape) subtype. This keeps the loader DSL-agnostic: no key-specific
     * assumptions (such as "a list of names is shorthand for a map") are baked in.
     *
     * @param sources the configured DSL sources
     * @return all successfully parsed documents
     * @throws IOException if a source directory cannot be listed
     */
    public List<ParsedDocument> loadAll(List<DslSource> sources) throws IOException {
        List<ParsedDocument> documents = new ArrayList<>();

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
                        documents.add(new ParsedDocument(source.name(), file, content));
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

        return documents;
    }
}
