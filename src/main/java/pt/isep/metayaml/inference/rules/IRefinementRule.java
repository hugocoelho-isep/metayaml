package pt.isep.metayaml.inference.rules;

import pt.isep.metayaml.model.InferredMetamodel;

/**
 * Contract for a refinement rule (R1–R3) in the Cánovas Izquierdo & Cabot algorithm.
 *
 * <p>Refinement rules operate on a fully-constructed {@link InferredMetamodel}
 * and improve its quality by adjusting optionality, types, and class structure.
 */
public interface IRefinementRule {
    /**
     * Applies the refinement to the given metamodel in-place.
     *
     * @param metamodel the metamodel to refine
     */
    void apply(InferredMetamodel metamodel);
}
