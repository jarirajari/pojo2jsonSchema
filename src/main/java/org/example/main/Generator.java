package org.example.main;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.tools.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        modelPaths.stream().forEach(model -> {
            try {
                String name = model.getFileName().toString();
                String filename = FilenameUtils.getBaseName(name);
                String extension = FilenameUtils.getExtension(name);
                Boolean compiled = compileJavaFile(Paths.get(String.format("models/org/openapitools/model/%s.%s", filename, extension)));
                URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File("models").toURI().toURL()}, ClassLoader.getSystemClassLoader());
                Class clazz = classLoader.loadClass(String.format("org.openapitools.model.%s", filename));
                JsonNode jsonSchema = jsonSchemaGenerator.generateJsonSchema(clazz);
                String jsonSchemaAsString = objectMapper.writeValueAsString(jsonSchema);
                generated.put(filename, jsonSchemaAsString);
                System.out.println(String.format("Generated model '%s' with '%s'", filename, jsonSchemaAsString));
            } catch (JsonProcessingException | ClassNotFoundException | MalformedURLException e) {
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
