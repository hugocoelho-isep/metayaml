package pt.isep.metayaml.config;

import java.nio.file.Path;
import java.util.List;

/**
 * Top-level application configuration loaded from metayaml.yml.
 *
 * @param outputDirectory where generated artefacts (.ecore, .puml) are written
 * @param sources         list of DSL types to process
 */
//public record AppConfig(Path inputDirectory, Path outputDirectory) {
public record AppConfig(Path outputDirectory, List<DslSource> sources) {

    public AppConfig {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Output directory must not be null");
        }
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("At least one DSL source must be configured");
        }
        sources = List.copyOf(sources); // immutable
    }
}
