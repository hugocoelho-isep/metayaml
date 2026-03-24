package pt.isep.metayaml.input;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class YamlFileDiscovery {

    private static final List<String> YAML_EXTENSIONS = List.of(".yml", ".yaml");

    /**
     * Returns all YAML files found directly inside {@code directory}.
     *
     * @param directory the directory to scan
     * @return sorted list of YAML file paths
     * @throws IOException if the directory cannot be read
     */
    public List<Path> discover(Path directory) throws IOException {
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(this::isYamlFile)
                    .sorted()
                    .toList();
        }
    }

    private boolean isYamlFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return YAML_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}

