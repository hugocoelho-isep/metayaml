package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.DataType;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.ArrayList;
import java.util.List;

/**
 * R5 — Empty class removal rule.
 *
 * <p>An empty class has no attributes and no references. These arise when a
 * YAML scalar is parsed as a mapping by SnakeYAML in some edge cases (e.g.
 * a string containing {@code :} being interpreted as a mapping key), causing
 * C2 to create a class with no content.
 *
 * <p>For each empty class, every reference pointing to it is replaced by a
 * plain {@code STRING} attribute with the same name. The empty class is then
 * removed from the metamodel.
 *
 * <p>Example — {@code group_id} inside a {@code with} block is a scalar but
 * was promoted to a class:
 * <pre>
 *   before: With --[group_id]--> Group_id {}
 *   after:  With.group_id : STRING [0..1]
 * </pre>
 */
public class R5_EmptyClassRemovalRule implements IRefinementRule {

    @Override
    public void apply(InferredMetamodel metamodel) {
        List<MetaClass> emptyClasses = metamodel.getClasses().stream()
                .filter(c -> c.getAttributes().isEmpty() && c.getReferences().isEmpty())
                .filter(c -> !c.isAbstract()) // abstract union supertypes are intentionally empty
                .toList();

        for (MetaClass empty : emptyClasses) {
            replaceReferencesWithAttribute(empty, metamodel);
            metamodel.removeClass(empty);
            System.out.printf("[INFO] R5_Empty: removed empty class '%s'%n", empty.getName());
        }
    }

    private void replaceReferencesWithAttribute(MetaClass empty, InferredMetamodel metamodel) {
        for (MetaClass cls : new ArrayList<>(metamodel.getClasses())) {
            List<MetaReference> toReplace = cls.getReferences().stream()
                    .filter(r -> r.getTarget() == empty)
                    .toList();

            for (MetaReference ref : toReplace) {
                cls.removeReference(ref.getName());
                if (cls.findAttribute(ref.getName()).isEmpty()) {
                    cls.addAttribute(new MetaAttribute(ref.getName(), DataType.STRING, true, ref.isMany()));
                }
                System.out.printf("[INFO] R5_Empty: replaced '%s.%s --> %s' with STRING attribute%n",
                        cls.getName(), ref.getName(), empty.getName());
            }
        }
    }
}
