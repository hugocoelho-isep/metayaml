package pt.isep.metayaml.export;

import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.MetaAttribute;
import pt.isep.metayaml.model.MetaClass;
import pt.isep.metayaml.model.MetaReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlantUmlExporter implements IMetamodelExporter{
    @Override
    public Path export(InferredMetamodel metamodel, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);

        String fileName = sanitize(metamodel.getDslName()) + ".puml";
        Path outputFile = outputDirectory.resolve(fileName);

        String content = generate(metamodel);
        Files.write(outputFile, content.getBytes());

        return outputFile;
    }

    private String generate(InferredMetamodel metamodel) {
        StringBuilder sb = new StringBuilder();

        sb.append("@startuml ").append(sanitize(metamodel.getDslName())).append("\n");
        sb.append("!theme plain\n");
        sb.append("skinparam classAttributeIconSize 0\n");
        sb.append("skinparam classFontStyle bold\n");
        sb.append("\n");

        // class definition
        for(MetaClass metaClass: metamodel.getClasses()){
            sb.append("class ").append(metaClass.getName()).append("{\n");
            for(MetaAttribute attr: metaClass.getAttributes()){
                sb.append("  ").append(formatAttribute(attr)).append("\n");
            }
            sb.append("}\n\n");
        }

        // relationships
        for(MetaClass metaClass: metamodel.getClasses()){
            for(MetaReference ref: metaClass.getReferences()){
                sb.append(formatRelationship(metaClass, ref)).append("\n");
            }
        }

        sb.append("\n@enduml\n");
        return sb.toString();
    }


    private String formatAttribute(MetaAttribute attr) {
        String multiplicity = attr.isMany()
                ? "[0..*]"
                : (attr.isOptional() ? "[0..1]" : "[1..1]");

        String typeName = attr.getType() == pt.isep.metayaml.model.DataType.MAP
                ? "Map<String, String>"
                : attr.getType().name();

        return "+ " + attr.getName() + " : " + typeName + " " + multiplicity;
    }

    private String formatRelationship(MetaClass owner, MetaReference ref) {
        String multiplicity = ref.isMany()
                ? (ref.isOptional() ? "\"0..*\"" : "\"1..*\"")
                : (ref.isOptional() ? "\"0..1\"" : "\"1..1\"");

        if(ref.isContainment()){
            // composition: filled diamond on owner side
            return owner.getName() + " *--> " + multiplicity + " " + ref.getTarget().getName() + " : " + ref.getName();
        } else {
            // association: open arrow
            return owner.getName() + " --> " + multiplicity + " " + ref.getTarget().getName() + " : " + ref.getName();
        }
    }

    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
