package org.example.main;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static java.nio.file.StandardOpenOption.*;

public class Application {
    private static final String USAGE = "\"Usage: java -jar p2js.jar --source <dir> --target <dir>\"";
    private static Generator generator = new Generator();
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println(USAGE);
            System.exit(1);
        }
        String inputDir = args[1];
        Path inputDirPath = Paths.get(inputDir);
        String outputDir = args[3];
        Path outputDirPath = Paths.get(outputDir);
        Boolean inputDirChecked = Files.isDirectory(inputDirPath) && Files.isReadable(inputDirPath);
        Boolean outputDirChecked = Files.isDirectory(outputDirPath) && Files.isWritable(outputDirPath);
        Boolean sourceArg = args[0].equals("--source"); // args[1] is dir
        Boolean targetArg = args[2].equals("--target"); // args[3] is dir
        if (inputDirChecked && sourceArg && outputDirChecked && targetArg) {
            // The files are copied to a temp directory! Target directory is where ONLY json schemas are written!
            List<Path> modelFilePaths = copyModelFilesToClassPathTemporarily(inputDirPath);
            Map<String, String> models = generator.generate(modelFilePaths);
            Boolean generated = writeGeneratedModels(outputDirPath, models);
            System.out.println(String.format("\nGenerated %d models! %s", models.keySet().size(), (generated) ? "" : "error!"));
            System.exit(0);
        } else {
            System.out.println(USAGE);
            System.exit(1);
        }
    }

    private static List<Path> copyModelFilesToClassPathTemporarily(Path inputDirPath) {
        List<Path> sources = listModelFiles(inputDirPath);
        List<Path> targets = new ArrayList<>();

        try {
            String sysTemp = System.getProperty("java.io.tmpdir");
            Path currentDir = Paths.get(sysTemp).toAbsolutePath();
            Path tempDir = Files.createTempDirectory(currentDir, "temp-pojo2jsonSchema-");
            sources.stream().forEach(source -> {

                try {
                    Path target = tempDir.resolve(source.getFileName());
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    targets.add(target.toAbsolutePath());
                } catch (IOException e) {
                    targets.clear();
                }
            });
        } catch (IOException e) {
            targets.clear();
        }

        return targets;
    }

    public static List<Path> listModelFiles(Path path) {
        List<Path> result;
        try {
            try (Stream<Path> walk = Files.walk(path)) {
                System.out.println("Searching....");
                result = walk.filter(Files::isRegularFile)
                        .filter(p ->  {
                            System.out.println(p);
                            String modelPattern = p.getFileName().toString().toLowerCase();
                            boolean isJava = modelPattern.endsWith(".java");
                            return isJava;
                        })
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            result = new ArrayList<>();
        }
        return result;
    }

    public static Boolean writeGeneratedModels(Path outputDir, Map<String, String> models) {
        final AtomicBoolean success = new AtomicBoolean(true);
        models.entrySet().stream().forEach(entry -> {
            String key = entry.getKey();
            String val = entry.getValue();
            try {
                String name = "" + key+".json";
                Path schemaDir = outputDir.resolve("schemas");
                FileUtils.forceMkdir(schemaDir.toFile());
                String outputFilename = schemaDir.resolve(name).toAbsolutePath().toString();
                System.out.println(String.format("Saving schema to '%s'", outputFilename));
                Path file = Paths.get(outputFilename);
                List<String> lines = Arrays.asList(val.split("\n"));
                Files.write(file, lines, CREATE, WRITE, TRUNCATE_EXISTING);
            } catch (IOException e) {
                success.set(false);
            }
        });
        return success.get();
    }
}
