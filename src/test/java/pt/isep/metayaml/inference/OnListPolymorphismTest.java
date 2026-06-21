package pt.isep.metayaml.inference;

import org.junit.jupiter.api.Test;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.ParsedDocument;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end check that the GitHub Actions {@code on:} key, which appears both as a
 * flow-sequence list ({@code on: [push, pull_request]}) and as a block mapping
 * ({@code on: { push: { ... } }}) across documents, is inferred as a typed union:
 * an abstract {@code On} supertype with an {@code OnObject} (the event-config map)
 * and an {@code OnValue} (the event list as a many-valued {@code value}) subtype.
 *
 * <p>This is the list-vs-object analogue of the scalar-vs-object cases
 * ({@code environment}, {@code permissions}, {@code matrix}).
 */
class OnListPolymorphismTest {

    private static ParsedDocument doc(String fileName, Map<String, Object> content) {
        return new ParsedDocument("github-actions", Path.of(fileName), content);
    }

    @Test
    void onAsListAndMappingInfersTypedUnion() {
        // on: [push, pull_request]  (flow-sequence list of scalars)
        ParsedDocument listForm = doc("list-form.yml", Map.of(
                "name", "List form",
                "on", List.of("push", "pull_request"),
                "jobs", Map.of(
                        "build", Map.of("runs-on", "ubuntu-latest"))
        ));

        // on: { push: { branches: [main] } }  (block mapping)
        ParsedDocument mapForm = doc("map-form.yml", Map.of(
                "name", "Map form",
                "on", Map.of(
                        "push", Map.of("branches", List.of("main"))),
                "jobs", Map.of(
                        "build", Map.of("runs-on", "ubuntu-latest"))
        ));

        InferredMetamodel mm = new MetamodelInferenceEngine()
                .infer("github-actions", List.of(listForm, mapForm));

        MetaClass on = mm.findClass("On").orElseThrow(() -> new AssertionError("On class missing"));
        assertTrue(on.isAbstract(), "On must be abstract (union supertype)");

        MetaClass onObject = mm.findClass("OnObject")
                .orElseThrow(() -> new AssertionError("OnObject subtype missing"));
        assertSame(on, onObject.getSuperType());
        assertTrue(onObject.findReference("push").isPresent(),
                "OnObject must keep the block-mapping event references");

        MetaClass onValue = mm.findClass("OnValue")
                .orElseThrow(() -> new AssertionError("OnValue subtype missing"));
        assertSame(on, onValue.getSuperType());
        MetaAttribute value = onValue.findAttribute("value")
                .orElseThrow(() -> new AssertionError("OnValue.value missing"));
        assertTrue(value.isMany(), "OnValue.value must be many-valued (events as EString[*])");

        // the root references the abstract union, not a leftover scalar/list attribute
        MetaClass root = mm.findClass("GithubActions").orElseThrow();
        assertTrue(root.findAttribute("on").isEmpty(), "no leftover list attribute on the root");
        assertSame(on, root.findReference("on").orElseThrow().getTarget());
    }
}
