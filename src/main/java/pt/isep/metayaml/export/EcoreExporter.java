package pt.isep.metayaml.export;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import pt.isep.metayaml.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;


/**
 * Exports an {@link InferredMetamodel} to an Eclipse Ecore file ({@code .ecore}).
 *
 * <p>The generated {@code .ecore} file can be opened directly in Eclipse with
 * the EMF plugin and used as the basis for Xtext grammar generation.
 *
 * <p>Mapping from MetaYAML model to Ecore:
 * <ul>
 *   <li>{@link InferredMetamodel} → {@link EPackage}
 *   <li>{@link MetaClass}         → {@link EClass}
 *   <li>{@link MetaAttribute}     → {@link EAttribute} with an {@link EDataType}
 *   <li>{@link MetaReference}     → {@link EReference} (containment or cross-reference)
 * </ul>
 */
public class EcoreExporter implements IMetamodelExporter{

    private static final String BASE_NS_URI = "http://www.isep.pt/metayaml/";

    private EDataType mapStringStringType;

    @Override
    public Path export(InferredMetamodel metamodel, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);

        String fileName = sanitize(metamodel.getDslName()) + ".ecore";
        Path outputPath = outputDirectory.resolve(fileName);

        EPackage ePackage = buildEPackage(metamodel);
        saveEcore(ePackage, outputPath);

        return outputPath;
    }

    // --- EPackage construction ---
    private EPackage buildEPackage(InferredMetamodel metamodel) {
        EcoreFactory factory = EcoreFactory.eINSTANCE;
        EPackage ePackage = factory.createEPackage();
        ePackage.setName(sanitize(metamodel.getDslName()));
        ePackage.setNsPrefix(sanitize(metamodel.getDslName()));
        ePackage.setNsURI(BASE_NS_URI + sanitize(metamodel.getDslName()));

        // register custom MapStringString EDataType for open-map attributes
        EDataType mapType = EcoreFactory.eINSTANCE.createEDataType();
        mapType.setName("MapStringString");
        mapType.setInstanceClassName("java.util.Map");
        ePackage.getEClassifiers().add(mapType);
        this.mapStringStringType = mapType;

        // pass 1: create all Eclasses first (needed for cross-references)
        Map<String, EClass> eClassMap = new HashMap<>();
        for (MetaClass metaClass: metamodel.getClasses()){
            EClass eClass = factory.createEClass();
            eClass.setName(metaClass.getName());
            ePackage.getEClassifiers().add(eClass);
            eClassMap.put(metaClass.getName(), eClass);
        }

        // pass 2: add attributes and references
        for(MetaClass metaClass: metamodel.getClasses()){
            EClass eClass = eClassMap.get(metaClass.getName());
            addAttributes(metaClass, eClass, factory);
            addReferences(metaClass, eClass, factory, eClassMap);
        }

        return ePackage;
    }


    private void addAttributes(MetaClass metaClass, EClass eClass, EcoreFactory factory) {
        for (MetaAttribute attribute: metaClass.getAttributes()) {
            EAttribute eAttribute = factory.createEAttribute();
            eAttribute.setName(sanitizeFeature(attribute.getName()));
            eAttribute.setEType(mapDataType(attribute.getType()));
            eAttribute.setLowerBound(attribute.isOptional() ? 0 : 1);
            eAttribute.setUpperBound(attribute.isMany() ? -1 : 1); // -1 = unbounded (0..*)
            eClass.getEStructuralFeatures().add(eAttribute);
        }
    }



    private void addReferences(MetaClass metaClass, EClass eClass, EcoreFactory factory, Map<String, EClass> eClassMap) {
        for (MetaReference reference: metaClass.getReferences()) {
            EClass targetEClass = eClassMap.get(reference.getTarget().getName());
            if (targetEClass == null) continue; // target was merged away

            EReference eReference = factory.createEReference();
            eReference.setName(sanitizeFeature(reference.getName()));
            eReference.setEType(targetEClass);
            eReference.setContainment(reference.isContainment());
            eReference.setLowerBound(reference.isOptional() ? 0 : 1);
            eReference.setUpperBound(reference.isMany() ? -1 : 1);
            eClass.getEStructuralFeatures().add(eReference);
        }
    }

    private EDataType mapDataType(DataType type) {
        EcorePackage ecore = EcorePackage.eINSTANCE;
        return switch (type) {
            case BOOLEAN -> ecore.getEBoolean();
            case INTEGER ->  ecore.getEInt();
            case FLOAT ->   ecore.getEFloat();
            case MAP -> mapStringStringType;
            default -> ecore.getEString();
        };
    }

    // EMF serialization
    private void saveEcore(EPackage ePackage, Path outputPath) throws IOException {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());

        URI uri = URI.createFileURI(outputPath.toAbsolutePath().toString());
        Resource resource = resourceSet.createResource(uri);
        resource.getContents().add(ePackage);

        resource.save(java.util.Collections.emptyMap());
    }

    // helpers
    private String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    /**
     * Sanitizes a feature name (attribute/reference) to a valid Java identifier.
     * e.g. "runs-on" → "runsOn", "pull_request" → "pullRequest"
     */
    private String sanitizeFeature(String name) {
        String[] parts = name.split("[-_]");
        if(parts.length == 1) return name;

        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if(!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                sb.append(parts[i].substring(1));
            }
        }
        return sb.toString();
    }
}
