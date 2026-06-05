package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;

import java.util.List;
import java.util.Set;

/**
 * R — Include/Exclude key-value-pair normalization rule.
 *
 * <p>GitHub Actions {@code include} and {@code exclude} entries inside a matrix
 * are ad-hoc key-value mappings — their keys mirror the matrix axis names and
 * vary freely across samples. The creation phase treats each observed key as a
 * fixed attribute, producing a sample-specific structure.
 *
 * <p>This rule replaces all scalar attributes on classes listed in
 * {@code FORCED_KVP_NAMES} with a single {@code entries: Map<String,String> [1..*]}
 * attribute. The {@link pt.isep.metayaml.export.EcoreExporter} already converts
 * {@code DataType.MAP} attributes into containment {@code EReference}s targeting
 * its hardcoded {@code KeyValuePair} class, so no exporter changes are required.
 *
 * <p>Example:
 * <pre>
 *   before: Include { os: STRING[1..1], c_compiler: STRING[1..1], cpp_compiler: STRING[1..1] }
 *   after:  Include { entries: Map&lt;String,String&gt; [1..*] }
 *   ecore:  Include --[entries]--&gt; KeyValuePair [1..*]
 * </pre>
 */
public class R_IncludeExcludeKvpRule implements IRefinementRule {

    private static final Set<String> FORCED_KVP_NAMES = Set.of("Include", "Exclude");

    @Override
    public void apply(InferredMetamodel metamodel) {
        for (MetaClass cls : metamodel.getClasses()) {
            if (!FORCED_KVP_NAMES.contains(cls.getName())) continue;
            if (cls.getAttributes().isEmpty()) continue;

            int attrCount = cls.getAttributes().size();
            List<String> attrNames = cls.getAttributes().stream()
                    .map(MetaAttribute::getName)
                    .toList();
            for (String name : attrNames) {
                cls.removeAttribute(name);
            }
            cls.addAttribute(new MetaAttribute("entries", DataType.MAP, false, true));
            System.out.printf("[INFO] R_IncludeExcludeKvp: transformed '%s' (%d attrs) to KeyValuePair entries%n",
                    cls.getName(), attrCount);
        }
    }
}
