package pt.isep.metayaml.input;

import pt.isep.metayaml.config.DslSource;
import pt.isep.metayaml.model.ParsedDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            documents.addAll(parseFiles(source.name(), files));
        }

        return documents;
    }

    private List<ParsedDocument> parseFiles(String dslName, List<Path> files) {
        List<ParsedDocument> results = new ArrayList<>();

        for (Path file : files) {
            try {
                ParsedDocument doc = parser.parse(dslName, file);
                results.add(doc);
                System.out.printf("[INFO] Parsed: [%s] %s%n", dslName, file.getFileName());
            } catch (YamlParseException e) {
                System.out.printf("[WARN] Skipped '%s': %s%n", file.getFileName(), e.getMessage());
            }
        }

        return results;
    }
}
