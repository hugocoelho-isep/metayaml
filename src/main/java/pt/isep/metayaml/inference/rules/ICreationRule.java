package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;

/**
 * Contract for a creation rule (C1–C5) in the Cánovas Izquierdo & Cabot algorithm.
 *
 * <p>Each rule inspects a YAML key-value pair and, if applicable, adds
 * attributes or references to the given {@link MetaClass}.
 */
public interface ICreationRule {
    /**
     * Returns true if this rule applies to the given value.
     *
     * @param value the YAML value associated with a key
     */
    boolean appliesTo(Object value);

    void apply(String key, Object value, MetaClass owner, InferredMetamodel metamodel, IRuleEngine engine);
}
