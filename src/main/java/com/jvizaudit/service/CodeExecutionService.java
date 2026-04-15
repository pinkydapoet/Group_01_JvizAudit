package com.jvizaudit.service;
import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CodeExecutionService {
    public String runCode(String code) {
        return runCode(code, "");
    }

    public String runCode(String code, String input) {
        try {
            // Trích xuất tên class động bằng Regex
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

            File temp = Files.createTempDirectory("jvizaudit").toFile();
            File src = new File(temp, className + ".java");
            Files.writeString(src.toPath(), code);
            
            // Compile
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            ProcessBuilder compileBuilder = new ProcessBuilder();
            compileBuilder.directory(temp);
            compileBuilder.redirectErrorStream(true);
            if (isWindows) {
                compileBuilder.command("cmd.exe", "/c", "javac " + className + ".java");
            } else {
                compileBuilder.command("sh", "-c", "javac " + className + ".java");
            }
            Process compileProcess = compileBuilder.start();
            if (!compileProcess.waitFor(10, TimeUnit.SECONDS)) {
                return "Compilation timeout";
            }
            if (compileProcess.exitValue() != 0) {
                return "Compilation Error:\n" + new String(compileProcess.getInputStream().readAllBytes());
            }
            
            // Run
            ProcessBuilder runBuilder = new ProcessBuilder();
            runBuilder.directory(temp);
            runBuilder.redirectErrorStream(true);
            if (isWindows) {
                runBuilder.command("cmd.exe", "/c", "java " + className);
            } else {
                runBuilder.command("sh", "-c", "java " + className);
            }
            Process runProcess = runBuilder.start();

            if (input != null && !input.isEmpty()) {
                try (OutputStream os = runProcess.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
                    writer.write(input);
                    if (!input.endsWith("\n")) {
                        writer.newLine();
                    }
                    writer.flush();
                } catch (Exception e) {
                }
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