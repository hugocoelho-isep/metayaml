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

    /**
     * Number of distinct child keys observed, across all documents of the current DSL,
     * inside mappings whose entries are all sub-mappings and whose key maps to the given
     * class name (e.g. {@code "Jobs"} → the distinct job names {@code build, test, …}).
     *
     * <p>Drives named-map-container detection: an open key space (user-defined instance
     * names) accumulates many distinct keys, whereas a fixed block keeps the same one or
     * two (e.g. {@code defaults → run}, {@code resources → limits, reservations}).
     */
    int distinctChildKeys(String className);
}
