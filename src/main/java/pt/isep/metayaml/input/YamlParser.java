package pt.isep.metayaml.input;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;
import pt.isep.metayaml.model.ParsedDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class YamlParser {
    private final Yaml yaml = createYaml();

    /**
     * Creates a SnakeYAML instance with boolean implicit resolution disabled,
     * so that YAML 1.1 boolean aliases (on/off/yes/no) are kept as plain strings.
     */
    private static Yaml createYaml() {
        LoaderOptions loaderOptions = new LoaderOptions();
        DumperOptions dumperOptions = new DumperOptions();
        Resolver resolver = new Resolver() {
            @Override
            public void addImplicitResolvers() {
                addImplicitResolver(Tag.NULL,  NULL,  "~nN\0");
                addImplicitResolver(Tag.INT,   INT,   "-+0123456789");
                addImplicitResolver(Tag.FLOAT, FLOAT, "-+0123456789.");
            }
        };
        return new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions, resolver);
    }

    /**
     * Parses the given YAML file.
     *
     * @param dslName    the DSL type this file belongs to
     * @param filePath   path to the YAML file
     * @return a {@link ParsedDocument} with the parsed content
     * @throws YamlParseException if the file cannot be read or has an unexpected structure
     */
    public ParsedDocument parse(String dslName, Path filePath) throws YamlParseException {
        try (InputStream in = Files.newInputStream(filePath)) {
            Object raw = yaml.load(in);
            Map<String, Object> content = asMapping(raw, filePath);
            return new ParsedDocument(dslName, filePath, content);
        } catch (IOException e) {
            throw new YamlParseException("Failed to read file: " + filePath, e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMapping(Object raw, Path filePath) throws YamlParseException {
        if (raw == null) {
            throw new YamlParseException("Empty YAML file: " + filePath);
        }
        if (!(raw instanceof Map)) {
            throw new YamlParseException(
                    "Expected a YAML mapping at the top level, but got " +
                            raw.getClass().getSimpleName() + ": " + filePath
            );
        }
        return (Map<String, Object>) raw;
    }
}
