package com.jvizaudit.service;
import com.github.javaparser.StaticJavaParser;
import org.springframework.stereotype.Service;

@Service
public class CodeFormatterService {
    public String format(String code) {
        try {
            return StaticJavaParser.parse(code).toString();
        } catch (Exception e) {
            return code; 
        }
    }
}