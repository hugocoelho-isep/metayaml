package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.List;
import java.util.Set;

/**
 * R — Open list-map detection rule.
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
 * <p>Detection heuristic: a class whose attributes are ALL many and optional,
 * with at least {@value #OPEN_LIST_MAP_THRESHOLD} such attributes, OR whose
 * name is in {@code FORCED_LIST_MAP_NAMES} (safety net for small sample sets
 * where fewer than 3 distinct axis names appear).
 *
 * <p>The threshold distinguishes open list-maps from structural multi-valued
 * fields: {@code Push} (branches, tags) and {@code Pull_request} (branches,
 * types) each have only 2 many-optional attributes and are not affected.
 *
 * <p>Example:
 * <pre>
 *   before: Matrix { os: STRING[0..*], node_version: STRING[0..*], ... (10 attrs) }
 *   after:  Matrix *--&gt; "0..*" MatrixParameter : parameters
 *           MatrixParameter { key: STRING[1..1], values: STRING[1..*] }
 * </pre>
 */
public class R_OpenListMapRule implements IRefinementRule {

    static final int OPEN_LIST_MAP_THRESHOLD = 3;

    private static final Set<String> FORCED_LIST_MAP_NAMES = Set.of("Matrix");

    @Override
    public void apply(InferredMetamodel metamodel) {
        List<MetaClass> targets = metamodel.getClasses().stream()
                .filter(this::isOpenListMap)
                .toList();

        if (targets.isEmpty()) return;

        MetaClass matrixParam = getOrCreateMatrixParameter(metamodel);

        for (MetaClass cls : targets) {
            int attrCount = cls.getAttributes().size();
            List<String> attrNames = cls.getAttributes().stream()
                    .map(MetaAttribute::getName)
                    .toList();
            for (String name : attrNames) {
                cls.removeAttribute(name);
            }
            cls.addReference(new MetaReference("parameters", matrixParam, true, true, true));
            System.out.printf("[INFO] R_OpenListMap: transformed '%s' (%d attrs) to MatrixParameter pattern%n",
                    cls.getName(), attrCount);
        }
    }

    private boolean isOpenListMap(MetaClass cls) {
        if (cls.getAttributes().isEmpty()) return false;
        boolean allManyOptional = cls.getAttributes().stream()
                .allMatch(a -> a.isMany() && a.isOptional());
        if (!allManyOptional) return false;
        if (FORCED_LIST_MAP_NAMES.contains(cls.getName())) return true;
        return cls.getAttributes().size() >= OPEN_LIST_MAP_THRESHOLD;
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
