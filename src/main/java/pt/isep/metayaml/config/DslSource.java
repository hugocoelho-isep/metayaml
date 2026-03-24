package pt.isep.metayaml.config;

import java.nio.file.Path;


/**
 * Represents a single DSL type configured for inference.
 * Each entry maps a human-readable name to a directory containing YAML samples.
 */
public record DslSource(String name, Path directory) {

    public DslSource{
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("DSL source name must not be blank");
        }
        if (directory == null) {
            throw new IllegalArgumentException("DSL source directory must not be null");
        }
    }
}
