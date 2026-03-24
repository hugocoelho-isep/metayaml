package pt.isep.metayaml.inference;

import pt.isep.metayaml.inference.rules.*;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.ParsedDocument;

import javax.swing.*;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the metamodel inference process.
 *
 * <p>Implements the creation phase of the Cánovas Izquierdo & Cabot (2013)
 * algorithm, applying rules C1–C5 recursively over each parsed YAML document.
 *
 * <p>Rules are evaluated in priority order: C1 and C4 (structural rules)
 * before C2 and C3 (scalar rules), with C5 as the fallback.
 */
public class MetamodelInferenceEngine implements IRuleEngine {

    private final List<ICreationRule> creationRules;
    private final List<IRefinementRule> refinementRules;

    public MetamodelInferenceEngine() {
        this.creationRules = List.of(
                new C4_MappingListRule(),
                new C1_MappingRule(),
                new C3_ScalarListRule(),
                new C2_ScalarRule(),
                new C5_MixedListRule()
        ); // The list of order must be on this order

       this.refinementRules = List.of(
               new R1_OptionalRule(),
               new R2_TypeRefinementRule(),
               new R3_ClassMergeRule()
       );
    }

    /**
     * Infers a metamodel from a list of parsed documents of the same DSL type.
     *
     * @param dslName   the DSL type name (e.g. "github-actions")
     * @param documents parsed YAML documents belonging to this DSL
     * @return the inferred metamodel
     */
    public InferredMetamodel infer(String dslName, List<ParsedDocument> documents) {
        InferredMetamodel metamodel = new InferredMetamodel(dslName);
        
        for(ParsedDocument doc : documents) {
            String rootClassName = deriveRootClassName(dslName);
            MetaClass rootClass = metamodel.getOrCreateClass(rootClassName);
            rootClass.incrementOccurrences();
            processMapping(doc.content(), rootClass, metamodel);
        }

        for(IRefinementRule rule: refinementRules){
            rule.apply(metamodel);
        }

        return metamodel;
    }

    @Override
    public void processMapping(Map<String, Object> mapping, MetaClass owner, InferredMetamodel metamodel) {
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            applyCreationRule(entry.getKey(), entry.getValue(), owner, metamodel);
        }
    }

    private void applyCreationRule(String key, Object value, MetaClass owner, InferredMetamodel metamodel) {
        for(ICreationRule rule: creationRules){
            if(rule.appliesTo(value)){
                rule.apply(key, value, owner, metamodel, this);
                return;
            }
        }
    }

    /**
     * Derives a root class name from the DSL name.
     * e.g. "github-actions" → "GithubActions", "docker-compose" → "DockerCompose"
     */
    private String deriveRootClassName(String dslName) {
        String[] parts = dslName.split("[-_]");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if(!part.isEmpty()){
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
