package pt.isep.metayaml.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ParsedDocument (String dslName, Path sourcePath, Map<String, Object> content){
    public ParsedDocument {
        if (dslName == null || dslName.isBlank()) {
            throw new IllegalArgumentException("DSL name must not be blank");
        }
        if (sourcePath == null) {
            throw new IllegalArgumentException("Source path must not be null");
        }
        if (content == null) {
            throw new IllegalArgumentException("Content must not be null");
        }
        // Preserve YAML insertion order (Map.copyOf randomises iteration order per JVM run,
        // which made class emission order — and thus the generated .ecore/.puml — non-deterministic).
        content = Collections.unmodifiableMap(new LinkedHashMap<>(content));
    }

    public String fileName() {
        return sourcePath.getFileName().toString();
    }
}
