package pt.isep.metayaml.export;

import pt.isep.metayaml.model.InferredMetamodel;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Contract for metamodel exporters.
 *
 * <p>Each implementation serializes an {@link InferredMetamodel} to a
 * specific format (e.g. Ecore, PlantUML) and writes the result to disk.
 */
public interface IMetamodelExporter {

    /**
     * Exports the given metamodel to the specified output directory.
     *
     * @param metamodel       the metamodel to export
     * @param outputDirectory the directory where the output file will be written
     * @return the path of the file that was written
     * @throws IOException if the file cannot be written
     */
    Path export(InferredMetamodel metamodel, Path outputDirectory) throws IOException;
}
