package com.jvizaudit.service.strategy;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UmlDiagramGenerator implements DiagramGenerator {
    private static final Logger logger = LoggerFactory.getLogger(UmlDiagramGenerator.class);
    
    public String generate(CompilationUnit ast) {
        try {
            StringBuilder sb = new StringBuilder("classDiagram\n");
            boolean hasClasses = false;
            boolean hasInheritance = false;
            
            for (TypeDeclaration<?> type : ast.getTypes()) {
                if (type instanceof ClassOrInterfaceDeclaration) {
                    hasClasses = true;
                    ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) type;
                    String className = c.getNameAsString();
                    
                    sb.append("  class ").append(className).append(" {\n");
                    
                    for (FieldDeclaration f : c.getFields()) {
                        String visibility = f.isPublic() ? "+" : f.isPrivate() ? "-" : "~";
                        String fieldType = f.getCommonType().asString();
                        String fieldName = f.getVariable(0).getNameAsString();
                        sb.append("    ").append(visibility).append(" ").append(fieldName).append(" : ").append(fieldType).append("\n");
                    }
                    
                    for (MethodDeclaration m : c.getMethods()) {
                        String visibility = m.isPublic() ? "+" : m.isPrivate() ? "-" : "~";
                        String methodName = m.getNameAsString();
                        String returnType = m.getType().asString();
                        sb.append("    ").append(visibility).append(" ").append(methodName).append("() : ").append(returnType).append("\n");
                    }
                    
                    sb.append("  }\n");
                    
                    for (ClassOrInterfaceType extended : c.getExtendedTypes()) {
                        sb.append("  ").append(extended.getNameAsString()).append(" <|-- ").append(className).append(" : extends\n");
                        hasInheritance = true;
                    }
                    for (ClassOrInterfaceType implemented : c.getImplementedTypes()) {
                        sb.append("  ").append(implemented.getNameAsString()).append(" <|.. ").append(className).append(" : implements\n");
                        hasInheritance = true;
                    }
                }
            }
            
            if (!hasClasses) {
                sb.append("  class Placeholder {\n");
                sb.append("    + placeholder() : void\n");
                sb.append("  }\n");
            }
            
            logger.info("UML Diagram generated" + (hasInheritance ? " with inheritance" : ""));
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error in UML generation: " + e.getMessage());
            return "classDiagram\n  class Error {\n    + checkSyntax() : void\n  }\n";  
        }
    }
    public String getType() { return "UML"; }
}