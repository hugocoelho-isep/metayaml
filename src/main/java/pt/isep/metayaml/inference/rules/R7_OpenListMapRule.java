package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.List;

/**
 * R7 — Open list-map detection rule.
 *
 * <p>Some YAML structures represent an open-key Cartesian-product space where
 * each key is a user-defined axis name with a list of possible values (e.g.
 * GitHub Actions {@code strategy.matrix}). The creation phase treats every
 * observed key as a fixed attribute, producing sample-specific names like
 * {@code os}, {@code node_version}, {@code python_version}.
 *
 * <p>This rule detects such classes and replaces their named many-valued
 * attributes with a generic {@code parameters: MatrixParameter [0..*]}
 * reference, where {@code MatrixParameter} holds a {@code key} and
 * {@code values} pair.
 *
 * <p>Detection is delegated to {@link OpenKeySpaceDetector#isOpenListMap}: a
 * class whose attributes are ALL many-valued and optional and where no axis key
 * appears in a majority of instances. This distinguishes open list-maps from
 * structural multi-valued classes: {@code Push} (branches, tags) and
 * {@code Pull_request} (branches, types) each keep a majority key (their main
 * field recurs in ~99% of instances) and are not affected.
 *
 * <p><b>Matrix refinement lists.</b> A matrix may carry {@code include} and
 * {@code exclude} lists whose entries reuse the matrix's open axis vocabulary.
 * These surface as scalar-only classes reached through a many-containment
 * reference from the matrix; statistically they look like fixed schemas (with so
 * few entries the keys happen to recur), so they are identified structurally —
 * as the matrix's own list children — and collapsed to {@code KeyValuePair}
 * entries, which the {@link pt.isep.metayaml.export.EcoreExporter} materialises
 * via the shared {@code KeyValuePair} class.
 *
 * <p>Example:
 * <pre>
 *   before: Matrix { os: STRING[0..*], node_version: STRING[0..*], ... (10 attrs) }
 *           Matrix *--&gt; "0..*" Include { os, c_compiler, ... }
 *   after:  Matrix *--&gt; "0..*" MatrixParameter : parameters
 *           MatrixParameter { key: STRING[1..1], values: STRING[1..*] }
 *           Include { entries: Map&lt;String,String&gt; [1..*] }
 * </pre>
 */
public class R7_OpenListMapRule implements IRefinementRule {

    @Override
    public void apply(InferredMetamodel metamodel) {
        List<MetaClass> targets = metamodel.getClasses().stream()
                .filter(OpenKeySpaceDetector::isOpenListMap)
                .toList();

        if (targets.isEmpty()) return;

        MetaClass matrixParam = getOrCreateMatrixParameter(metamodel);

        for (MetaClass cls : targets) {
            // Snapshot the matrix's scalar-only list children (include/exclude) before
            // we add the synthetic 'parameters' reference, so MatrixParameter is excluded.
            List<MetaClass> refinementLists = cls.getReferences().stream()
                    .filter(r -> r.isMany() && r.isContainment())
                    .map(MetaReference::getTarget)
                    .filter(R7_OpenListMapRule::isScalarOnly)
                    .toList();

            int attrCount = cls.getAttributes().size();
            for (String name : attributeNames(cls)) {
                cls.removeAttribute(name);
            }
            cls.addReference(new MetaReference("parameters", matrixParam, true, true, true));
            System.out.printf("[INFO] R7_OpenListMap: transformed '%s' (%d attrs) to MatrixParameter pattern%n",
                    cls.getName(), attrCount);

            for (MetaClass refinement : refinementLists) {
                collapseToKvpEntries(refinement);
            }
        }
    }

    /** A class with at least one attribute and no outgoing references (e.g. matrix include/exclude entries). */
    private static boolean isScalarOnly(MetaClass cls) {
        return !cls.getAttributes().isEmpty() && cls.getReferences().isEmpty();
    }

    private void collapseToKvpEntries(MetaClass cls) {
        if (cls.findAttribute("entries").isPresent()) return;
        int attrCount = cls.getAttributes().size();
        for (String name : attributeNames(cls)) {
            cls.removeAttribute(name);
        }
        cls.addAttribute(new MetaAttribute("entries", DataType.MAP, false, true));
        System.out.printf("[INFO] R7_OpenListMap: collapsed matrix refinement '%s' (%d attrs) to KeyValuePair entries%n",
                cls.getName(), attrCount);
    }

    private static List<String> attributeNames(MetaClass cls) {
        return cls.getAttributes().stream().map(MetaAttribute::getName).toList();
    }

    private MetaClass getOrCreateMatrixParameter(InferredMetamodel metamodel) {
        return metamodel.findClass("MatrixParameter").orElseGet(() -> {
            MetaClass mp = new MetaClass("MatrixParameter");
            mp.addAttribute(new MetaAttribute("key",    DataType.STRING, false, false));
            mp.addAttribute(new MetaAttribute("values", DataType.STRING, false, true));
            metamodel.addClass(mp);
            return mp;
        });
    }
}
