package pt.isep.metayaml;

import pt.isep.metayaml.config.AppConfig;
import pt.isep.metayaml.config.ConfigException;
import pt.isep.metayaml.config.ConfigLoader;
import pt.isep.metayaml.export.EcoreExporter;
import pt.isep.metayaml.export.IMetamodelExporter;
import pt.isep.metayaml.export.PlantUmlExporter;
import pt.isep.metayaml.inference.MetamodelInferenceEngine;
import pt.isep.metayaml.input.InputLoader;
import pt.isep.metayaml.model.InferredMetamodel;
import pt.isep.metayaml.model.ParsedDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Path configPath = resolveConfig(args);

        ConfigLoader configLoader = new ConfigLoader();
        AppConfig config;
        try  {
            config = configLoader.load(configPath);
        } catch (ConfigException e) {
            System.err.println("[ERROR] Configuration error: " + e.getMessage());
            System.exit(1);
            return;
        }

        InputLoader inputLoader = new InputLoader();
        List<ParsedDocument> documents;
        try {
            documents = inputLoader.loadAll(config.sources());
        } catch(IOException e) {
            System.err.println("[ERROR] Failed to load input files: " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.printf("%n[INFO] Loaded %d document(s) from %d source(s).%n",
                documents.size(), config.sources().size());


        // group documents by DSL name and infer one metamodel per source
        MetamodelInferenceEngine engine = new MetamodelInferenceEngine();
        Map<String, List<ParsedDocument>> byDsl = documents.stream()
                .collect(Collectors.groupingBy(ParsedDocument::dslName));

        List<IMetamodelExporter> exporters = List.of(
                new PlantUmlExporter(),
                new EcoreExporter()
        );

        for (Map.Entry<String, List<ParsedDocument>> entry : byDsl.entrySet()) {
            InferredMetamodel metamodel = engine.infer(entry.getKey(), entry.getValue());
            System.out.printf("%n[INFO] %s%n", metamodel);
            metamodel.getClasses().forEach(c -> {
                System.out.printf("  %s%n", c);
                c.getAttributes().forEach(a -> System.out.printf("    attr: %s%n", a));
                c.getReferences().forEach(r -> System.out.printf("    ref:  %s%n", r));
            });

            // export
            for(IMetamodelExporter exporter : exporters) {
                try{
                    Path output = exporter.export(metamodel, config.outputDirectory());
                    System.out.printf("[INFO] Exported %s%n", output);
                } catch (IOException e) {
                    System.err.printf("[ERROR] Export failed for '%s': %s%n",
                            entry.getKey(), e.getMessage());
                }
            }
        }
    }

    private static Path resolveConfig(String[] args) {
        if (args.length > 0) {
            return Path.of(args[0]);
        }

        Path defaultConfig = Path.of("metayaml.yml");
        if (defaultConfig.toFile().exists()) {
            return defaultConfig;
        }

        System.err.println("Usage: metayaml <config-file>");
        System.err.println();
        System.err.println("  No argument provided and no metayaml.yml found in current directory.");
        System.err.println("  Create a metayaml.yml file or pass the path explicitly.");
        System.err.println();
        System.err.println("  Example metayaml.yml:");
        System.err.println("    output: ./output");
        System.err.println("    sources:");
        System.err.println("      - name: github-actions");
        System.err.println("        directory: ./samples/github-actions");
        System.exit(1);
        return null; // unreachable
    }
}