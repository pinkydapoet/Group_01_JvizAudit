package com.jvizaudit.service.strategy;
import com.github.javaparser.ast.CompilationUnit;
public interface DiagramGenerator {
    String generate(CompilationUnit ast);
    String getType();
}