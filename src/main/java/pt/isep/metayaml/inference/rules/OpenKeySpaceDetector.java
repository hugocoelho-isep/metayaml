package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;

/**
 * Domain-agnostic detection of <em>open key spaces</em> — YAML structures whose
 * keys are defined by the user rather than fixed by a schema (e.g. {@code env},
 * {@code with}, {@code outputs}, or a {@code strategy.matrix}).
 *
 * <p>The creation phase records, per class, how many instances were observed
 * ({@link MetaClass#getOccurrences()}) and, per attribute, in how many of those
 * instances each key appeared ({@link MetaAttribute#getOccurrences()}). That is
 * all the evidence needed to tell an open key space from a fixed schema without
 * hardcoding any DSL-specific class name:
 *
 * <ul>
 *   <li><b>Fixed schema</b> — at least one key is near-universal. {@code Push}
 *       has {@code branches} in 98% of pushes; {@code Step} has a key in 82% of
 *       steps; a permissions object has {@code contents} in 95%.</li>
 *   <li><b>Open key space</b> — <em>no</em> key reaches a majority. The most
 *       common matrix axis appears in 30% of matrices, the most common env var
 *       in under 9% of env blocks.</li>
 * </ul>
 *
 * <p>The discriminator is therefore {@link #maxKeyRecurrence(MetaClass)}: the
 * share of instances in which the most frequent key appears. A class is an open
 * key space when that share stays below {@link #MAJORITY} — "no key occurs in a
 * majority of instances". This is independent of the absolute number of keys
 * (works for {@code outputs} with 4 keys and {@code with} with 300) and robust
 * as long as there are several samples; with a single sample no statistic can
 * distinguish the two cases.
 */
final class OpenKeySpaceDetector {

    /** A key seen in at least this share of instances counts as a majority (schema) key. */
    static final double MAJORITY = 0.5;

    private OpenKeySpaceDetector() {
    }

    /** Share of instances (0..1) in which the most frequently observed attribute appears. */
    static double maxKeyRecurrence(MetaClass cls) {
        if (cls.getAttributes().isEmpty()) return 0.0;
        int classOccurrences = Math.max(cls.getOccurrences(), 1);
        int maxOccurrences = cls.getAttributes().stream()
                .mapToInt(MetaAttribute::getOccurrences)
                .max()
                .orElse(0);
        return (double) maxOccurrences / classOccurrences;
    }

    /** Attributes are all optional and no key reaches a majority of instances. */
    private static boolean noMajorityKey(MetaClass cls) {
        return !cls.getAttributes().isEmpty()
                && cls.getAttributes().stream().allMatch(MetaAttribute::isOptional)
                && maxKeyRecurrence(cls) < MAJORITY;
    }

    /**
     * Open single-valued map (e.g. {@code env}, {@code with}, {@code outputs}):
     * a leaf class (no outgoing references) whose attributes are all
     * single-valued and optional, with no majority key.
     */
    static boolean isOpenScalarMap(MetaClass cls) {
        return cls.getReferences().isEmpty()
                && cls.getAttributes().stream().noneMatch(MetaAttribute::isMany)
                && noMajorityKey(cls);
    }

    /**
     * Open list-map (e.g. {@code strategy.matrix}): all attributes are
     * many-valued and optional, with no majority axis key. Outgoing references
     * (e.g. the matrix {@code include}/{@code exclude} refinement lists) are
     * allowed and left untouched here.
     */
    static boolean isOpenListMap(MetaClass cls) {
        return cls.getAttributes().stream().allMatch(MetaAttribute::isMany)
                && noMajorityKey(cls);
    }
}
