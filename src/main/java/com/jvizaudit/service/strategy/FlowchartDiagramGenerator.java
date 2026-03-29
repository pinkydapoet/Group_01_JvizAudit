package com.jvizaudit.service.strategy;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class FlowchartDiagramGenerator implements DiagramGenerator {
    private static final Logger logger = LoggerFactory.getLogger(FlowchartDiagramGenerator.class);
    private int nodeCounter = 0;
    
    public String generate(CompilationUnit ast) {
        try {
            StringBuilder sb = new StringBuilder("graph TD\n");
            nodeCounter = 0;
            
            sb.append("  classDef startEnd fill:#4CAF50,stroke:#45a049,stroke-width:2px,color:#fff\n");
            sb.append("  classDef process fill:#2196F3,stroke:#0b7dda,stroke-width:2px,color:#fff\n");
            sb.append("  classDef decision fill:#FF9800,stroke:#e68900,stroke-width:2px,color:#fff\n");
            sb.append("  classDef method fill:#9C27B0,stroke:#7b1fa2,stroke-width:2px,color:#fff\n\n");
            
            sb.append("  Start([START]):::startEnd\n");
            String currentNode = "Start";
            
            boolean hasFlow = false;
            for (TypeDeclaration<?> type : ast.getTypes()) {
                if (type instanceof ClassOrInterfaceDeclaration) {
                    ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) type;
                    if (!c.getMethods().isEmpty()) {
                        MethodDeclaration firstMethod = c.getMethods().get(0);
                        currentNode = processStatements(sb, firstMethod.getBody().orElse(null), currentNode);
                        hasFlow = true;
                    }
                }
            }
            
            if (!hasFlow) {
                String node = "Node" + (nodeCounter++);
                sb.append("  ").append(node).append("[\"No executable flow\"]:::process\n");
                sb.append("  Start --> ").append(node).append("\n");
                currentNode = node;
            }
            
            sb.append("  ").append(currentNode).append(" --> End\n");
            sb.append("  End([END]):::startEnd\n");
            
            logger.info("Flowchart with execution flow generated");
            return sb.toString();
        } catch (Exception e) {
            logger.error("Error in Flowchart: " + e.getMessage());
            return "graph TD\n" +
                   "  classDef startEnd fill:#4CAF50,stroke:#45a049,stroke-width:2px,color:#fff\n" +
                   "  Start([START]):::startEnd\n" +
                   "  Start --> End([END]):::startEnd\n";
        }
    }
    
    private String processStatements(StringBuilder sb, BlockStmt block, String previousNode) {
        if (block == null) return previousNode;
        
        String currentNode = previousNode;
        
        for (Statement stmt : block.getStatements()) {
            if (stmt.isExpressionStmt()) {
                String nodeId = "Node" + (nodeCounter++);
                String label = stmt.toString().replace("\"", "'").substring(0, Math.min(30, stmt.toString().length()));
                sb.append("  ").append(nodeId).append("[\"").append(label).append("\"]:::process\n");
                sb.append("  ").append(currentNode).append(" --> ").append(nodeId).append("\n");
                currentNode = nodeId;
            } 
            else if (stmt instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) stmt;
                String decisionNode = "Decision" + (nodeCounter++);
                String condition = ifStmt.getCondition().toString().replace("\"", "'");
                sb.append("  ").append(decisionNode).append("{\"").append(condition).append("\"}:::decision\n");
                sb.append("  ").append(currentNode).append(" --> ").append(decisionNode).append("\n");
                
                String thenNode = processStatements(sb, ifStmt.getThenStmt().isBlockStmt() ? ifStmt.getThenStmt().asBlockStmt() : null, decisionNode);
                
                if (ifStmt.hasElseBranch()) {
                    String elseNode = processStatements(sb, ifStmt.getElseStmt().get().isBlockStmt() ? ifStmt.getElseStmt().get().asBlockStmt() : null, decisionNode);
                    currentNode = elseNode;
                } else {
                    currentNode = thenNode;
                }
            }
            else if (stmt instanceof ForStmt) {
                ForStmt forStmt = (ForStmt) stmt;
                String loopNode = "Loop" + (nodeCounter++);
                String loopCondition = forStmt.getCompare().map(Object::toString).orElse("loop");
                sb.append("  ").append(loopNode).append("{\"").append(loopCondition).append("\"}:::decision\n");
                sb.append("  ").append(currentNode).append(" --> ").append(loopNode).append("\n");
                
                String bodyNode = processStatements(sb, forStmt.getBody().isBlockStmt() ? forStmt.getBody().asBlockStmt() : null, loopNode);
                sb.append("  ").append(bodyNode).append(" --> ").append(loopNode).append("\n");
                currentNode = loopNode;
            }
            else if (stmt instanceof WhileStmt) {
                WhileStmt whileStmt = (WhileStmt) stmt;
                String loopNode = "While" + (nodeCounter++);
                String condition = whileStmt.getCondition().toString().replace("\"", "'");
                sb.append("  ").append(loopNode).append("{\"").append(condition).append("\"}:::decision\n");
                sb.append("  ").append(currentNode).append(" --> ").append(loopNode).append("\n");
                
                String bodyNode = processStatements(sb, whileStmt.getBody().isBlockStmt() ? whileStmt.getBody().asBlockStmt() : null, loopNode);
                sb.append("  ").append(bodyNode).append(" --> ").append(loopNode).append("\n");
                currentNode = loopNode;
            }
            else if (stmt instanceof ReturnStmt) {
                String returnNode = "Return" + (nodeCounter++);
                sb.append("  ").append(returnNode).append("[\"return\"]:::process\n");
                sb.append("  ").append(currentNode).append(" --> ").append(returnNode).append("\n");
                currentNode = returnNode;
            }
        }
        
        return currentNode;
    }
    
    public String getType() { return "FLOWCHART"; }
}