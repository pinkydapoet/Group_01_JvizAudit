package com.jvizaudit.service;

import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeExecutionService {

    public String runCode(String code, String input) {
        String className = "Main"; 
        Matcher matcher = Pattern.compile("public\\s+class\\s+([A-Za-z0-9_]+)").matcher(code);
        if (matcher.find()) {
            className = matcher.group(1);
        } else {
            Matcher classMatcher = Pattern.compile("class\\s+([A-Za-z0-9_]+)").matcher(code);
            if (classMatcher.find()) {
                className = classMatcher.group(1);
            }
        }
        return runWorkspace(Map.of(className + ".java", code), className, input);
    }

    public String runWorkspace(Map<String, String> files, String mainClass, String input) {
        try {
            File tempDir = Files.createTempDirectory("jvizaudit_workspace").toFile();
            
            for (Map.Entry<String, String> entry : files.entrySet()) {
                File file = new File(tempDir, entry.getKey());
                file.getParentFile().mkdirs();
                Files.writeString(file.toPath(), entry.getValue());
            }
            
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            
            ProcessBuilder compileBuilder = new ProcessBuilder();
            compileBuilder.directory(tempDir);
            compileBuilder.redirectErrorStream(true);
            
            if (isWindows) {
                compileBuilder.command("cmd.exe", "/c", "dir /s /B *.java > sources.txt && javac @sources.txt");
            } else {
                compileBuilder.command("sh", "-c", "find . -name \"*.java\" > sources.txt && javac @sources.txt");
            }
            
            Process compileProcess = compileBuilder.start();
            if (!compileProcess.waitFor(10, TimeUnit.SECONDS)) {
                return "Compilation timeout";
            }
            if (compileProcess.exitValue() != 0) {
                return "Compilation Error:\n" + new String(compileProcess.getInputStream().readAllBytes());
            }
            
            ProcessBuilder runBuilder = new ProcessBuilder();
            runBuilder.directory(tempDir);
            runBuilder.redirectErrorStream(true);
            if (isWindows) {
                runBuilder.command("cmd.exe", "/c", "java " + mainClass);
            } else {
                runBuilder.command("sh", "-c", "java " + mainClass);
            }
            Process runProcess = runBuilder.start();

            if (input != null && !input.isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
                    writer.write(input);
                    if (!input.endsWith("\n")) writer.newLine();
                    writer.flush();
                } catch (Exception e) {}
            }

            if (!runProcess.waitFor(5, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                return "Execution timeout (Infinite loop or waiting for input)";
            }
            
            String output = new String(runProcess.getInputStream().readAllBytes());
            return output.isEmpty() ? "(No output)" : output;
        } catch (Exception e) { 
            return "Execution Error: " + e.getMessage(); 
        }
    }
}