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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
     * Parses the given YAML file and returns a mutable content map with null
     * values replaced by empty maps. Used by {@link InputLoader} for two-pass
     * normalization before wrapping in an immutable {@link ParsedDocument}.
     *
     * @param filePath path to the YAML file
     * @return mutable content map
     * @throws YamlParseException if the file cannot be read or has an unexpected structure
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseToMap(Path filePath) throws YamlParseException {
        try (InputStream in = Files.newInputStream(filePath)) {
            Object raw = yaml.load(in);
            Map<String, Object> content = asMapping(raw, filePath);
            normalizeNulls(content);
            return content;
        } catch (IOException e) {
            throw new YamlParseException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * Parses the given YAML file into an immutable {@link ParsedDocument}.
     *
     * @param dslName  the DSL type this file belongs to
     * @param filePath path to the YAML file
     * @return a {@link ParsedDocument} with the parsed content
     * @throws YamlParseException if the file cannot be read or has an unexpected structure
     */
    public ParsedDocument parse(String dslName, Path filePath) throws YamlParseException {
        Map<String, Object> content = parseToMap(filePath);
        return new ParsedDocument(dslName, filePath, content);
    }

    /**
     * Parses the given YAML file into one or more root mappings.
     *
     * <p>Most configuration formats (e.g. GitHub Actions, Docker Compose) have a
     * single mapping at the top level. Some formats use a top-level sequence whose
     * elements are mappings. In that case each element is returned as a separate
     * root mapping, so the inference engine treats every element as an independent
     * document of the same DSL.
     *
     * @param filePath path to the YAML file
     * @return one root mapping for a mapping-rooted file, or one per element for a
     *         sequence-rooted file
     * @throws YamlParseException if the file cannot be read, is empty, or has a top
     *         level that is neither a mapping nor a sequence of mappings
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> parseToRootMaps(Path filePath) throws YamlParseException {
        try (InputStream in = Files.newInputStream(filePath)) {
            Object raw = yaml.load(in);
            List<Map<String, Object>> roots = new ArrayList<>();
            if (raw == null) {
                throw new YamlParseException("Empty YAML file: " + filePath);
            } else if (raw instanceof Map) {
                Map<String, Object> content = (Map<String, Object>) raw;
                normalizeNulls(content);
                roots.add(content);
            } else if (raw instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<String, Object> content = (Map<String, Object>) item;
                        normalizeNulls(content);
                        roots.add(content);
                    }
                }
                if (roots.isEmpty()) {
                    throw new YamlParseException(
                            "Top-level sequence contains no mappings: " + filePath);
                }
            } else {
                throw new YamlParseException(
                        "Expected a YAML mapping or sequence of mappings at the top level, but got "
                                + raw.getClass().getSimpleName() + ": " + filePath);
            }
            return roots;
        } catch (IOException e) {
            throw new YamlParseException("Failed to read file: " + filePath, e);
        }
    }

    /**
     * Recursively replaces null or empty-string values in the YAML tree with
     * empty maps so that bare keys (e.g. {@code workflow_dispatch:}) are treated
     * as empty objects rather than scalars by the inference engine.
     *
     * <p>Note: the custom resolver disables full YAML 1.1 null resolution, so
     * SnakeYAML represents bare scalar values as empty strings ("") instead of
     * Java {@code null}. Both cases are normalised here.
     *
     * <p>SnakeYAML can produce maps with non-String keys (complex flow-style keys);
     * those entries are visited recursively but their keys are left untouched.
     */
    @SuppressWarnings("unchecked")
    private void normalizeNulls(Map<?, ?> map) {
        Map<Object, Object> rawMap = (Map<Object, Object>) map;
        List<Object> keys = new ArrayList<>(rawMap.keySet());
        for (Object keyObj : keys) {
            Object val = rawMap.get(keyObj);
            if (val == null || "".equals(val)) {
                rawMap.put(keyObj, new LinkedHashMap<>());
            } else if (val instanceof Map<?, ?> nested) {
                normalizeNulls(nested);
            } else if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> mapItem) {
                        normalizeNulls(mapItem);
                    }
                }
            }
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
