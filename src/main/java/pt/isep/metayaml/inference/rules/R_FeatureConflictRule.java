package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;

import java.util.List;

/**
 * R — Feature conflict resolution rule.
 *
 * <p>The same YAML key can appear as a scalar value in some documents and as a
 * nested mapping in others. The creation rules (C1, C2) accumulate features
 * independently, which can leave a class with both a scalar attribute and a
 * containment reference sharing the same name.
 *
 * <p>The reference is always more informative: it captures structure. This rule
 * removes every attribute whose name is already taken by a reference on the same
 * class. The reference's optionality is correctly handled later by R1.
 *
 * <p>Example — {@code GithubActions.on} appears as {@code on: push} (scalar) in
 * some files and as {@code on: {push: {branches: [main]}}} (mapping) in others:
 * <pre>
 *   before: GithubActions.on : STRING  AND  GithubActions --[on]--> On
 *   after:  GithubActions --[on]--> On  (attribute removed)
 * </pre>
 */
public class R_FeatureConflictRule implements IRefinementRule {

    @Override
    public void apply(InferredMetamodel metamodel) {
        for (MetaClass cls : metamodel.getClasses()) {
            List<String> conflicts = cls.getAttributes().stream()
                    .map(a -> a.getName())
                    .filter(name -> cls.findReference(name).isPresent())
                    .toList();

            for (String name : conflicts) {
                cls.findAttribute(name).ifPresent(attr ->
                    cls.findReference(name).ifPresent(ref -> {
                        for (int i = 0; i < attr.getOccurrences(); i++) ref.incrementOccurrences();
                    })
                );
                cls.removeAttribute(name);
                System.out.printf("[INFO] R_Conflict: removed attribute '%s.%s' (shadowed by reference)%n",
                        cls.getName(), name);
            }
        }
    }
}