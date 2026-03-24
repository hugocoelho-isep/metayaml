package pt.isep.metayaml.model;

import java.nio.file.Path;
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
        content = Map.copyOf(content); // immutable
    }

    public String fileName() {
        return sourcePath.getFileName().toString();
    }
}
