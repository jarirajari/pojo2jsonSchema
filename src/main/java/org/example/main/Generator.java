package org.example.main;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.tools.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Generator {
    /*
    // HTML5 config:
    // JsonSchemaGenerator html5 = new JsonSchemaGenerator(objectMapper, JsonSchemaConfig.html5EnabledSchema() );
    //
    // Manual config:
    // JsonSchemaConfig config = JsonSchemaConfig.create(...);
    // JsonSchemaGenerator generator = new JsonSchemaGenerator(objectMapper, config);
    */
    public Map<String, String> generate(List<Path> modelPaths) {
        ObjectMapper objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);
        JsonSchemaGenerator jsonSchemaGenerator = new JsonSchemaGenerator(objectMapper);
        Map<String, String> generated = new HashMap<>();

        modelPaths.stream().forEach(modelPath -> {
            try {
                String name = modelPath.getFileName().toString();
                String filename = FilenameUtils.getBaseName(name);
                String extension = FilenameUtils.getExtension(name);
                String directory = modelPath.getParent().toAbsolutePath().toString();
                // Pojos (.java files) need to be compiled into classes (in a temp directory)
                CompilationUnit cu = StaticJavaParser.parse(modelPath.toFile());
                ClassOrInterfaceDeclaration classDeclaration = cu.findFirst(ClassOrInterfaceDeclaration.class).orElse(null);
                String className = classDeclaration != null ? classDeclaration.getNameAsString() : "";
                cu.removePackageDeclaration();
                String updatedCode = cu.toString();
                Files.write(modelPath, updatedCode.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
                Boolean compiled = compileJavaFile(modelPath);
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(directory).toURI().toURL()}, ClassLoader.getSystemClassLoader());
                Class clazz = classLoader.loadClass(className);
                // Generate the JSON Schema => .json files are written to target path
                JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(clazz);
                String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
                generated.put(filename, jsonSchemaAsString);
                System.out.println(String.format("Generated modelPath '%s' with '%s'", filename, jsonSchemaAsString));
            } catch (JsonProcessingException | ClassNotFoundException | MalformedURLException | FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        modelPaths.stream().forEach(l -> System.out.println(l));
        MapUtils.debugPrint(System.out, "generated", generated);
        return generated;
    }

    // NOTE! Each compiled .class file should be compiled into claaspath!
    public Boolean compileJavaFile(Path javaFile) {
        Boolean compiled = true;
        try {
            // Get the system's JavaCompiler
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            // Diagnostics
            DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
            StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);
            // Compile the Java source file
            int compilationResult = compiler.run(null, null, null, javaFile.toString());
            if (compilationResult != 0) {
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
                    System.err.println(diagnostic.toString());
                }
                compiled = false;
            }
        } catch (RuntimeException e) {
            compiled = false;
        }
        return compiled;
    }
}
