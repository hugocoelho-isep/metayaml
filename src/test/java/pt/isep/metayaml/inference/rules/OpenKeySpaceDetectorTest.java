package pt.isep.metayaml.inference.rules;

import org.junit.jupiter.api.Test;
import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks in the statistical open-key-space heuristic that replaced the hardcoded
 * {@code FORCED_*} class-name sets, independently of the sample corpus. The
 * numbers mirror the real metamodel measured on the 179 GitHub Actions samples.
 */
class OpenKeySpaceDetectorTest {

    private static MetaClass cls(String name, int occurrences) {
        MetaClass c = new MetaClass(name);
        for (int i = 0; i < occurrences; i++) c.incrementOccurrences();
        return c;
    }

    private static void addAttr(MetaClass c, String name, int occurrences, boolean optional, boolean many) {
        MetaAttribute a = new MetaAttribute(name, DataType.STRING, optional, many);
        for (int i = 1; i < occurrences; i++) a.incrementOccurrences(); // constructor starts at 1
        c.addAttribute(a);
    }

    @Test
    void maxKeyRecurrenceIsShareOfMostFrequentKey() {
        MetaClass c = cls("Env", 80);
        addAttr(c, "JAVA_HOME", 7, true, false);   // 7/80
        addAttr(c, "NODE_ENV", 2, true, false);    // 2/80
        assertEquals(7.0 / 80.0, OpenKeySpaceDetector.maxKeyRecurrence(c), 1e-9);
    }

    @Test
    void emptyClassHasZeroRecurrence() {
        assertEquals(0.0, OpenKeySpaceDetector.maxKeyRecurrence(cls("Empty", 5)));
    }

    @Test
    void openScalarMapDetectedWhenNoKeyReachesMajority() {
        // env: many sparse, all-optional, single-valued, no references
        MetaClass env = cls("Env", 80);
        addAttr(env, "A", 7, true, false);
        addAttr(env, "B", 3, true, false);
        addAttr(env, "C", 1, true, false);
        assertTrue(OpenKeySpaceDetector.isOpenScalarMap(env));
    }

    @Test
    void fixedSchemaWithMajorityKeyIsNotOpen() {
        // a permissions-like object: 10 optional keys but one near-universal (0.95)
        MetaClass perms = cls("PermissionsObject", 191);
        addAttr(perms, "contents", 181, true, false); // 0.948 -> majority
        addAttr(perms, "issues", 30, true, false);
        addAttr(perms, "packages", 12, true, false);
        assertFalse(OpenKeySpaceDetector.isOpenScalarMap(perms));
    }

    @Test
    void classWithOutgoingReferenceIsNotAScalarMap() {
        MetaClass strategy = cls("Strategy", 13);
        addAttr(strategy, "fail_fast", 1, true, false);
        strategy.addReference(new MetaReference("matrix", cls("Matrix", 10), true, true, false));
        assertFalse(OpenKeySpaceDetector.isOpenScalarMap(strategy));
    }

    @Test
    void manyValuedAttributesAreNotAScalarMap() {
        MetaClass push = cls("Push", 160);
        addAttr(push, "branches", 1, true, true); // many -> excluded from scalar map
        assertFalse(OpenKeySpaceDetector.isOpenScalarMap(push));
    }

    @Test
    void openListMapDetectedForSparseManyValuedAxes() {
        // matrix axes: all many-valued, optional, none reaches majority
        MetaClass matrix = cls("MatrixObject", 10);
        addAttr(matrix, "os", 3, true, true);            // 0.30
        addAttr(matrix, "node_version", 2, true, true);
        addAttr(matrix, "python_version", 1, true, true);
        assertTrue(OpenKeySpaceDetector.isOpenListMap(matrix));
    }

    @Test
    void structuralMultiValuedClassIsNotAListMap() {
        // Push(branches, tags): both many-valued but branches is near-universal (0.99)
        MetaClass push = cls("Push", 160);
        addAttr(push, "branches", 158, true, true); // 0.988 -> majority key
        addAttr(push, "tags", 80, true, true);
        assertFalse(OpenKeySpaceDetector.isOpenListMap(push));
    }

    @Test
    void mandatoryKeysDisqualifyOpenDetection() {
        // a key seen in every instance (not optional) means a fixed schema
        MetaClass c = cls("Concurrency", 9);
        addAttr(c, "group", 9, false, false);
        addAttr(c, "cancel_in_progress", 9, false, false);
        assertFalse(OpenKeySpaceDetector.isOpenScalarMap(c));
    }
}
