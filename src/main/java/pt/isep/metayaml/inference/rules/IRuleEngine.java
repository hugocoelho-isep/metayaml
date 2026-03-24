package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;

import java.util.Map;

/**
 * Passed to {@link ICreationRule} implementations to allow recursive
 * processing of nested YAML structures without coupling rules to the engine.
 */
public interface IRuleEngine {
    /**
     * Processes all key-value pairs of a YAML mapping into the given MetaClass.
     *
     * @param mapping   the YAML mapping to process
     * @param owner     the MetaClass to populate
     * @param metamodel the metamodel being constructed
     */
    void processMapping(Map<String, Object> mapping, MetaClass owner, InferredMetamodel metamodel);
}
