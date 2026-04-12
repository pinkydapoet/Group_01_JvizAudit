package com.jvizaudit.service.strategy;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
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
                    for (MethodDeclaration method : c.getMethods()) {
                        if (method.getBody().isPresent()) {
                            String methodNode = createProcessNode(sb, "Method: " + method.getNameAsString());
                            sb.append("  ").append(currentNode).append(" --> ").append(methodNode).append("\n");
                            currentNode = methodNode;
                            currentNode = processStatements(sb, method.getBody().get(), currentNode);
                            hasFlow = true;
                        }
                    }
                }
            }

            if (!hasFlow) {
                String node = createProcessNode(sb, "No executable flow");
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
            currentNode = processStatement(sb, stmt, currentNode, null);
        }
        return currentNode;
    }

    private String processStatement(StringBuilder sb, Statement stmt, String previousNode, String edgeLabel) {
        if (stmt.isExpressionStmt()) {
            String nodeId = createProcessNode(sb, formatLabel(stmt.toString()));
            createEdge(sb, previousNode, nodeId, edgeLabel);
            return nodeId;
        }
        if (stmt.isReturnStmt()) {
            String nodeId = createProcessNode(sb, "return");
            createEdge(sb, previousNode, nodeId, edgeLabel);
            return nodeId;
        }
        if (stmt.isContinueStmt()) {
            String continueNode = createProcessNode(sb, "continue");
            createEdge(sb, previousNode, continueNode, edgeLabel);
            return "CONTINUE:" + continueNode;
        }
        if (stmt.isBreakStmt()) {
            String breakNode = createProcessNode(sb, "break");
            createEdge(sb, previousNode, breakNode, edgeLabel);
            return "BREAK:" + breakNode;
        }
        if (stmt.isIfStmt()) {
            return processIfStatement(sb, stmt.asIfStmt(), previousNode);
        }
        if (stmt.isForStmt()) {
            return processLoopStatement(sb, stmt.asForStmt(), previousNode);
        }
        if (stmt.isForEachStmt()) {
            return processForEachStatement(sb, stmt.asForEachStmt(), previousNode);
        }
        if (stmt.isWhileStmt()) {
            return processWhileStatement(sb, stmt.asWhileStmt(), previousNode);
        }
        if (stmt.isTryStmt()) {
            return processTryStatement(sb, stmt.asTryStmt(), previousNode);
        }

        String nodeId = createProcessNode(sb, formatLabel(stmt.toString()));
        createEdge(sb, previousNode, nodeId, edgeLabel);
        return nodeId;
    }

    private String processIfStatement(StringBuilder sb, IfStmt ifStmt, String previousNode) {
        String decisionNode = createDecisionNode(sb, formatLabel(ifStmt.getCondition().toString()));
        createEdge(sb, previousNode, decisionNode, null);

        String thenExit = processBranch(sb, ifStmt.getThenStmt(), decisionNode, "Yes", null, null);
        String elseExit = ifStmt.getElseStmt().isPresent()
                ? processBranch(sb, ifStmt.getElseStmt().get(), decisionNode, "No", null, null)
                : null;

        String mergeNode = "Node" + (nodeCounter++);
        sb.append("  ").append(mergeNode).append("[\" \"]:::process\n"); // Sử dụng " " thay vì rỗng hoàn toàn

        if (thenExit != null) {
            sb.append("  ").append(thenExit).append(" --> ").append(mergeNode).append("\n");
        } else {
            sb.append("  ").append(decisionNode).append(" -->|Yes| ").append(mergeNode).append("\n");
        }

        if (elseExit != null) {
            sb.append("  ").append(elseExit).append(" --> ").append(mergeNode).append("\n");
        } else {
            sb.append("  ").append(decisionNode).append(" -->|No| ").append(mergeNode).append("\n");
        }

        return mergeNode;
    }

    private String processLoopStatement(StringBuilder sb, ForStmt loopStmt, String previousNode) {
        String loopNode = createDecisionNode(sb, formatLabel(loopStmt.getCompare().map(Object::toString).orElse("loop")));
        createEdge(sb, previousNode, loopNode, null);

        String afterLoop = "Node" + (nodeCounter++);
        sb.append("  ").append(afterLoop).append("[\" \"]:::process\n");

        String bodyExit = processBranch(sb, loopStmt.getBody(), loopNode, "Yes", loopNode, afterLoop);
        if (bodyExit == null) {
            sb.append("  ").append(loopNode).append(" -->|Yes| ").append(loopNode).append("\n");
        } else {
            sb.append("  ").append(bodyExit).append(" --> ").append(loopNode).append("\n");
        }

        sb.append("  ").append(loopNode).append(" -->|No| ").append(afterLoop).append("\n");
        return afterLoop;
    }

    private String processWhileStatement(StringBuilder sb, WhileStmt whileStmt, String previousNode) {
        String loopNode = createDecisionNode(sb, formatLabel(whileStmt.getCondition().toString()));
        createEdge(sb, previousNode, loopNode, null);

        String afterLoop = "Node" + (nodeCounter++);
        sb.append("  ").append(afterLoop).append("[\" \"]:::process\n");

        String bodyExit = processBranch(sb, whileStmt.getBody(), loopNode, "Yes", loopNode, afterLoop);
        if (bodyExit == null) {
            sb.append("  ").append(loopNode).append(" -->|Yes| ").append(loopNode).append("\n");
        } else {
            sb.append("  ").append(bodyExit).append(" --> ").append(loopNode).append("\n");
        }

        sb.append("  ").append(loopNode).append(" -->|No| ").append(afterLoop).append("\n");
        return afterLoop;
    }

    private String processForEachStatement(StringBuilder sb, ForEachStmt forEachStmt, String previousNode) {
        String loopNode = createDecisionNode(sb, formatLabel(forEachStmt.getIterable().toString()));
        createEdge(sb, previousNode, loopNode, null);

        String afterLoop = "Node" + (nodeCounter++);
        sb.append("  ").append(afterLoop).append("[\" \"]:::process\n");

        String bodyExit = processBranch(sb, forEachStmt.getBody(), loopNode, "Yes", loopNode, afterLoop);
        if (bodyExit == null) {
            sb.append("  ").append(loopNode).append(" -->|Yes| ").append(loopNode).append("\n");
        } else {
            sb.append("  ").append(bodyExit).append(" --> ").append(loopNode).append("\n");
        }

        sb.append("  ").append(loopNode).append(" -->|No| ").append(afterLoop).append("\n");
        return afterLoop;
    }

    private String processTryStatement(StringBuilder sb, TryStmt tryStmt, String previousNode) {
        String tryNode = createProcessNode(sb, "try");
        createEdge(sb, previousNode, tryNode, null);

        String bodyExit = processStatements(sb, tryStmt.getTryBlock(), tryNode);
        String afterTry = "Node" + (nodeCounter++);
        sb.append("  ").append(afterTry).append("[\" \"]:::process\n");

        if (bodyExit != null && !bodyExit.equals(tryNode)) {
            sb.append("  ").append(bodyExit).append(" --> ").append(afterTry).append("\n");
        } else {
            sb.append("  ").append(tryNode).append(" --> ").append(afterTry).append("\n");
        }

        for (CatchClause catchClause : tryStmt.getCatchClauses()) {
            String catchNode = createDecisionNode(sb, formatLabel(catchClause.getParameter().toString()));
            sb.append("  ").append(tryNode).append(" -->|Exception| ").append(catchNode).append("\n");
            String catchExit = processStatements(sb, catchClause.getBody(), catchNode);
            if (catchExit != null) {
                sb.append("  ").append(catchExit).append(" --> ").append(afterTry).append("\n");
            } else {
                sb.append("  ").append(catchNode).append(" --> ").append(afterTry).append("\n");
            }
        }

        if (tryStmt.getFinallyBlock().isPresent()) {
            afterTry = processStatements(sb, tryStmt.getFinallyBlock().get(), afterTry);
        }
        return afterTry;
    }

    private String processBranch(StringBuilder sb, Statement stmt, String previousNode, String edgeLabel, String loopDecisionNode, String afterLoopNode) {
        if (stmt == null) {
            return null;
        }
        if (stmt.isBlockStmt()) {
            BlockStmt block = stmt.asBlockStmt();
            if (block.isEmpty()) {
                return null;
            }
            String lastNode = previousNode;
            boolean first = true;
            for (Statement inner : block.getStatements()) {
                String result = processStatement(sb, inner, lastNode, first ? edgeLabel : null);
                if (result != null && result.startsWith("CONTINUE:")) {
                    String continueNode = result.substring(9);
                    if (loopDecisionNode != null) {
                        sb.append("  ").append(continueNode).append(" --> ").append(loopDecisionNode).append("\n");
                    }
                    return null; 
                } else if (result != null && result.startsWith("BREAK:")) {
                    String breakNode = result.substring(6);
                    if (afterLoopNode != null) {
                        sb.append("  ").append(breakNode).append(" --> ").append(afterLoopNode).append("\n");
                    }
                    return null; 
                } else if (result != null) {
                    lastNode = result;
                }
                first = false;
            }
            return lastNode;
        }
        String result = processStatement(sb, stmt, previousNode, edgeLabel);
        if (result != null && result.startsWith("CONTINUE:")) {
            String continueNode = result.substring(9);
            if (loopDecisionNode != null) {
                sb.append("  ").append(continueNode).append(" --> ").append(loopDecisionNode).append("\n");
            }
            return null;
        } else if (result != null && result.startsWith("BREAK:")) {
            String breakNode = result.substring(6);
            if (afterLoopNode != null) {
                sb.append("  ").append(breakNode).append(" --> ").append(afterLoopNode).append("\n");
            }
            return null;
        }
        return result;
    }

    private String createProcessNode(StringBuilder sb, String label) {
        String nodeId = "Node" + (nodeCounter++);
        sb.append("  ").append(nodeId).append("[\"").append(label).append("\"]:::process\n");
        return nodeId;
    }

    private String createDecisionNode(StringBuilder sb, String condition) {
        String nodeId = "Decision" + (nodeCounter++);
        sb.append("  ").append(nodeId).append("{\"").append(condition).append("\"}:::decision\n");
        return nodeId;
    }

    private void createEdge(StringBuilder sb, String source, String target, String label) {
        if (label != null && !label.isEmpty()) {
            sb.append("  ").append(source).append(" -->|").append(label).append("| ").append(target).append("\n");
        } else {
            sb.append("  ").append(source).append(" --> ").append(target).append("\n");
        }
    }

    private String formatLabel(String text) {
        String sanitized = text.replace("\"", "")
                               .replace("'", "")
                               .replace("<", "&lt;")
                               .replace(">", "&gt;")
                               .replaceAll("\\s+", " ")
                               .trim();
        if (sanitized.length() > 40) {
            sanitized = sanitized.substring(0, 40) + "...";
        }
        return sanitized;
    }

    public String getType() { return "FLOWCHART"; }
}