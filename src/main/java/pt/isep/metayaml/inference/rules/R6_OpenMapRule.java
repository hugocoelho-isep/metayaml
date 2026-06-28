package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.ArrayList;
import java.util.List;

/**
 * R6 — Open map detection rule.
 *
 * <p>Some YAML keys represent open maps — structures where any string key is
 * valid (e.g. {@code env} accepts any environment variable name, {@code with}
 * accepts any action input). The creation phase treats each observed key as a
 * fixed attribute, accumulating hundreds of entries across many sample files.
 *
 * <p>This rule identifies "open map" classes via {@link OpenKeySpaceDetector}:
 * a leaf class (no outgoing references) whose attributes are all single-valued,
 * optional, and where no key appears in a majority of instances. Such classes
 * are collapsed: every inbound reference is replaced by a {@code MAP} attribute
 * on the owner class, and the class itself is removed.
 *
 * <p>Example:
 * <pre>
 *   before: GithubActions *--&gt; Env   (Env has 150+ optional STRING attrs)
 *   after:  GithubActions.env : MAP [0..1]
 * </pre>
 *
 * <p><b>Polymorphic open maps.</b> When the same key appears as both an open map
 * and a scalar/list (e.g. Docker Compose {@code environment:}, written either as a
 * {@code {KEY: value}} mapping or a {@code [KEY=value]} list),
 * {@link R1_PolymorphicFeatureRule} runs first and turns it into an abstract
 * supertype with {@code <Name>Object} (the open map) and {@code <Name>Value}
 * subtypes, retargeting the parent reference to the supertype. The open map is
 * then only reachable through inheritance, so a naive collapse would delete it
 * without leaving a replacement. This rule detects that case and collapses the
 * <em>whole union</em> — supertype plus both subtypes — into a single {@code MAP}
 * attribute on every parent, preserving the map form.
 */
public class R6_OpenMapRule implements IRefinementRule {

    @Override
    public void apply(InferredMetamodel metamodel) {
        List<MetaClass> openMaps = metamodel.getClasses().stream()
                .filter(this::isOpenMap)
                .toList();

        for (MetaClass openMap : openMaps) {
            MetaClass supertype = openMap.getSuperType();
            if (supertype != null && supertype.isAbstract()) {
                // The open map is the <Name>Object arm of a polymorphic union. Parent
                // references point at the abstract supertype, so redirect those, then drop
                // the supertype and all of its subtypes (Object + Value).
                collapseToMapAttribute(supertype, metamodel);
                List<MetaClass> subtypes = metamodel.getClasses().stream()
                        .filter(c -> c.getSuperType() == supertype)
                        .toList();
                for (MetaClass sub : subtypes) {
                    metamodel.removeClass(sub);
                }
                metamodel.removeClass(supertype);
                System.out.printf("[INFO] R6_OpenMap: collapsed polymorphic open-map union '%s' (via '%s', %d attrs) to MAP attribute%n",
                        supertype.getName(), openMap.getName(), openMap.getAttributes().size());
            } else {
                collapseToMapAttribute(openMap, metamodel);
                metamodel.removeClass(openMap);
                System.out.printf("[INFO] R6_OpenMap: collapsed '%s' (%d attrs) to MAP attribute%n",
                        openMap.getName(), openMap.getAttributes().size());
            }
        }
    }

    private boolean isOpenMap(MetaClass cls) {
        return OpenKeySpaceDetector.isOpenScalarMap(cls);
    }

    private void collapseToMapAttribute(MetaClass openMap, InferredMetamodel metamodel) {
        for (MetaClass cls : new ArrayList<>(metamodel.getClasses())) {
            List<MetaReference> toReplace = cls.getReferences().stream()
                    .filter(r -> r.getTarget() == openMap)
                    .toList();

            for (MetaReference ref : toReplace) {
                cls.removeReference(ref.getName());
                if (cls.findAttribute(ref.getName()).isEmpty()) {
                    cls.addAttribute(new MetaAttribute(ref.getName(), DataType.MAP, ref.isOptional(), ref.isMany()));
                }
            }
        }
    }
}
