package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.util.HashMap;
import java.util.Map;

/**
 * R3 — Shared class optional rule.
 *
 * <p>A class referenced by two or more distinct parent classes is a
 * "shared" or "utility" class (e.g. {@code Env}, {@code With},
 * {@code Permissions}). Its presence in any given parent is inherently
 * optional — the inference algorithm cannot distinguish mandatory from
 * optional based solely on the available samples when a class is reused
 * across many contexts.
 *
 * <p>This rule marks every reference that points to a shared class as
 * optional, preventing sampling bias from producing spurious mandatory
 * multiplicities.
 *
 * <p>Example — {@code Env} is referenced by {@code GithubActions},
 * {@code Job}, {@code Step} and {@code Service}. Even if all sampled
 * services happened to declare {@code env}, R2 would have marked
 * {@code Service.env} as mandatory {@code [1..1]}. This rule corrects
 * that to {@code [0..1]}.
 */
public class R3_SharedClassOptionalRule implements IRefinementRule {

    private static final int MIN_PARENTS = 2;

    @Override
    public void apply(InferredMetamodel metamodel) {
        Map<MetaClass, Integer> parentCount = countParents(metamodel);

        for (MetaClass cls : metamodel.getClasses()) {
            for (MetaReference ref : cls.getReferences()) {
                if (!ref.isOptional() && parentCount.getOrDefault(ref.getTarget(), 0) >= MIN_PARENTS) {
                    ref.setOptional(true);
                    System.out.printf("[INFO] R3_Shared: '%s.%s' marked optional (shared by %d classes)%n",
                            cls.getName(), ref.getName(), parentCount.get(ref.getTarget()));
                }
            }
        }
    }

    private Map<MetaClass, Integer> countParents(InferredMetamodel metamodel) {
        Map<MetaClass, Integer> count = new HashMap<>();
        for (MetaClass cls : metamodel.getClasses()) {
            cls.getReferences().stream()
                    .map(MetaReference::getTarget)
                    .distinct()
                    .forEach(target -> count.merge(target, 1, Integer::sum));
        }
        return count;
    }
}
