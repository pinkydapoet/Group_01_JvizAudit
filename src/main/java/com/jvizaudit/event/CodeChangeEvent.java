package com.jvizaudit.event;
import org.springframework.context.ApplicationEvent;

public class CodeChangeEvent extends ApplicationEvent {
    private String sessionId;
    private String sourceCode;

    public CodeChangeEvent(Object source, String sessionId, String sourceCode) {
        super(source);
        this.sessionId = sessionId;
        this.sourceCode = sourceCode;
    }
    public String getSessionId() { return sessionId; }
    public String getSourceCode() { return sourceCode; }
}