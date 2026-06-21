package pt.isep.metayaml.input;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pt.isep.metayaml.config.DslSource;
import pt.isep.metayaml.inference.MetamodelInferenceEngine;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.ParsedDocument;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full-pipeline tests for {@link InputLoader}.
 *
 * <p>The loader must parse YAML faithfully and must NOT rewrite one shape into
 * another (e.g. coerce a scalar-list into a map). Shape differences for the same
 * key across documents are reconciled downstream by the inference engine's
 * polymorphism rule, not by the loader — this keeps the loader DSL-agnostic.
 */
class InputLoaderTest {

    private static void write(Path dir, String name, String content) throws IOException {
        Files.writeString(dir.resolve(name), content);
    }

    @Test
    void scalarListsArePreservedNotCoercedToMaps(@TempDir Path dir) throws IOException {
        // same key 'on' appears as a flow-sequence list in one file and a mapping in another
        write(dir, "list.yml", "name: list\non: [push, pull_request]\n");
        write(dir, "map.yml", "name: map\non:\n  push:\n    branches: [main]\n");

        List<ParsedDocument> docs = new InputLoader()
                .loadAll(List.of(new DslSource("gha", dir)));

        assertEquals(2, docs.size());

        Object listForm = docs.stream()
                .filter(d -> d.fileName().equals("list.yml"))
                .findFirst().orElseThrow()
                .content().get("on");
        assertInstanceOf(List.class, listForm, "scalar list must stay a List, not be coerced to a Map");
        assertEquals(List.of("push", "pull_request"), listForm);

        Object mapForm = docs.stream()
                .filter(d -> d.fileName().equals("map.yml"))
                .findFirst().orElseThrow()
                .content().get("on");
        assertInstanceOf(Map.class, mapForm, "mapping form must stay a Map");
    }

    @Test
    void listVsMapKeyBecomesTypedUnionThroughFullPipeline(@TempDir Path dir) throws IOException {
        write(dir, "list.yml", "name: list\non: [push, pull_request]\njobs:\n  build:\n    runs-on: ubuntu-latest\n");
        write(dir, "map.yml", "name: map\non:\n  push:\n    branches: [main]\njobs:\n  build:\n    runs-on: ubuntu-latest\n");

        List<ParsedDocument> docs = new InputLoader()
                .loadAll(List.of(new DslSource("github-actions", dir)));

        InferredMetamodel mm = new MetamodelInferenceEngine().infer("github-actions", docs);

        MetaClass on = mm.findClass("On").orElseThrow(() -> new AssertionError("On class missing"));
        assertTrue(on.isAbstract(), "On must be an abstract union supertype");
        MetaClass onObject = mm.findClass("OnObject").orElseThrow(() -> new AssertionError("OnObject missing"));
        assertTrue(onObject.findReference("push").isPresent(), "OnObject keeps the mapping-form event references");
        MetaClass onValue = mm.findClass("OnValue").orElseThrow(() -> new AssertionError("OnValue missing"));
        assertTrue(onValue.findAttribute("value").orElseThrow().isMany(),
                "OnValue.value carries the list events as EString[*]");
    }
}
