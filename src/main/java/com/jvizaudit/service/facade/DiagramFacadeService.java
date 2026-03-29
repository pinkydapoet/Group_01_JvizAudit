package com.jvizaudit.service.facade;
import com.github.javaparser.StaticJavaParser;
import com.jvizaudit.service.strategy.DiagramGenerator;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

@Service
public class DiagramFacadeService {
    private static final Logger logger = LoggerFactory.getLogger(DiagramFacadeService.class);
    private List<DiagramGenerator> generators;

    public DiagramFacadeService(List<DiagramGenerator> generators) {
        this.generators = generators;
        logger.info("Registered diagram generators: " + generators.size());
    }

    public String generateDiagram(String code, String type) {
        try {
            logger.info("Generating " + type + " diagram");
            if (code == null || code.trim().isEmpty()) {
                return getFallbackDiagram(type);
            }
            
            var ast = StaticJavaParser.parse(code);
            for (DiagramGenerator g : generators) {
                if (g.getType().equals(type)) {
                    String diagram = g.generate(ast);
                    logger.info("Generated " + type + " diagram successfully");
                    return diagram;
                }
            }
            logger.warn("No generator found for type: " + type);
            return getFallbackDiagram(type);
        } catch (Exception e) {
            logger.error("Error generating " + type + " diagram: " + e.getMessage());
            return getFallbackDiagram(type);
        }
    }
    
    private String getFallbackDiagram(String type) {
        if ("UML".equals(type)) {
            return "classDiagram\n  class Placeholder {\n    placeholder()\n  }";
        } else if ("FLOWCHART".equals(type)) {
            return "graph TD;\n  Start([Start]) --> Process[Write Code];\n  Process --> End([End])";
        }
        return "graph TD;\n  Start([Start]) --> End([End])";
    }
}