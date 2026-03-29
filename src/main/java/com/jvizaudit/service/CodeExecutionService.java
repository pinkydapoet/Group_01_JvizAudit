package com.jvizaudit.service;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {
    public String runCode(String code) {
        try {
            // Create temp directory
            File temp = Files.createTempDirectory("jvizaudit").toFile();
            File src = new File(temp, "Main.java");
            Files.writeString(src.toPath(), code);
            
            // Step 1: Compile
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            ProcessBuilder compileBuilder = new ProcessBuilder();
            compileBuilder.directory(temp);
            compileBuilder.redirectErrorStream(true);
            if (isWindows) {
                compileBuilder.command("cmd.exe", "/c", "javac Main.java");
            } else {
                compileBuilder.command("sh", "-c", "javac Main.java");
            }
            Process compileProcess = compileBuilder.start();
            if (!compileProcess.waitFor(10, TimeUnit.SECONDS)) {
                return "Compilation timeout";
            }
            if (compileProcess.exitValue() != 0) {
                return "Compilation Error:\n" + new String(compileProcess.getInputStream().readAllBytes());
            }
            
            // Step 2: Run
            ProcessBuilder runBuilder = new ProcessBuilder();
            runBuilder.directory(temp);
            runBuilder.redirectErrorStream(true);
            if (isWindows) {
                runBuilder.command("cmd.exe", "/c", "java Main");
            } else {
                runBuilder.command("sh", "-c", "java Main");
            }
            Process runProcess = runBuilder.start();
            if (!runProcess.waitFor(5, TimeUnit.SECONDS)) {
                runProcess.destroyForcibly();
                return "Execution timeout";
            }
            
            String output = new String(runProcess.getInputStream().readAllBytes());
            return output.isEmpty() ? "(No output)" : output;
        } catch (Exception e) { 
            return "Execution Error: " + e.getMessage(); 
        }
    }
    
    public String generateFlowchart(String code) {
        return "graph TD;\n  START([Start]) --> PROCESS[Parse Code];\n  PROCESS --> END([End]);";
    }
}