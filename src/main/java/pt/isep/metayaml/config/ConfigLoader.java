package pt.isep.metayaml.config;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ConfigLoader {

    private static final String KEY_OUTPUT    = "output";
    private static final String KEY_SOURCES   = "sources";
    private static final String KEY_NAME      = "name";
    private static final String KEY_DIRECTORY = "directory";

    /**
     * Loads the configuration from the given file path.
     *
     * @param configPath path to metayaml.yml
     * @return parsed and validated {@link AppConfig}
     * @throws ConfigException if the file cannot be read or is malformed
     */
    public AppConfig load(Path configPath) throws ConfigException {
        validateFileExists(configPath);

        Map<String, Object> raw = parseYaml(configPath);

        Path outputDirectory = resolveOutput(raw, configPath);
        List<DslSource>   sources = resolveSources(raw, configPath);

        return new AppConfig(outputDirectory, sources);
    }


    private void validateFileExists(Path path) throws ConfigException {
        if (!Files.exists(path)) {
            throw new ConfigException("Configuration file not found: " + path.toAbsolutePath());
        }
        if (!Files.isRegularFile(path)) {
            throw new ConfigException("Configuration path is not a file: " + path.toAbsolutePath());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(Path configPath) throws ConfigException {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(configPath)) {
            Object parsed = yaml.load(in);
            if (!(parsed instanceof Map)) {
                throw new ConfigException("Configuration file must be a YAML mapping: " + configPath);
            }
            return (Map<String, Object>) parsed;
        } catch (IOException e) {
            throw new ConfigException("Failed to read configuration file: " + configPath, e);
        }
    }

    private Path resolveOutput(Map<String, Object> raw, Path configPath) throws ConfigException {
        Object value = raw.get(KEY_OUTPUT);
        if (value == null) {
            throw new ConfigException("Missing required field '" + KEY_OUTPUT + "' in configuration");
        }
        Path base = configPath.toAbsolutePath().getParent();
        return base.resolve(value.toString()).normalize();
    }

    @SuppressWarnings("unchecked")
    private List<DslSource> resolveSources(Map<String, Object> raw, Path configPath) throws ConfigException {
        Object value = raw.get(KEY_SOURCES);
        if (!(value instanceof List<?> list)) {
            throw new ConfigException("Missing or invalid field '" + KEY_SOURCES + "' in configuration");
        }

        List<DslSource> sources = new java.util.ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object entry = list.get(i);
            if (!(entry instanceof Map<?, ?> entryMap)) {
                throw new ConfigException("Source entry #" + (i + 1) + " must be a mapping");
            }
            sources.add(parseDslSource((Map<String, Object>) entryMap, configPath, i + 1));
        }

        if (sources.isEmpty()) {
            throw new ConfigException("At least one source must be defined under '" + KEY_SOURCES + "'");
        }
        return sources;
    }

    private DslSource parseDslSource(Map<String, Object> entry, Path configPath, int index) throws ConfigException {
        String name = getString(entry, KEY_NAME, "source #" + index);
        String dir  = getString(entry, KEY_DIRECTORY, "source '" + name + "'");

        Path base = configPath.toAbsolutePath().getParent();
        Path directory = base.resolve(dir).normalize();
        if (!Files.isDirectory(directory)) {
            throw new ConfigException(
                    "Directory for source '" + name + "' does not exist or is not a directory: " + directory
            );
        }
        return new DslSource(name, directory);
    }

    private String getString(Map<String, Object> map, String key, String context) throws ConfigException {
        Object value = map.get(key);
        if (value == null || value.toString().isBlank()) {
            throw new ConfigException("Missing required field '" + key + "' in " + context);
        }
        return value.toString().trim();
    }
}
