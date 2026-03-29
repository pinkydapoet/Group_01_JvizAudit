package com.jvizaudit.controller;
import com.jvizaudit.event.CodeChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.Principal;

@Controller
public class CodeWebSocketController {
    private static final Logger logger = LoggerFactory.getLogger(CodeWebSocketController.class);
    
    @Autowired
    private ApplicationEventPublisher publisher;

    @MessageMapping("/code.update")
    public void receiveCodeUpdate(String code, SimpMessageHeaderAccessor header) {
        String sessionId = header.getSessionId();
        Principal principal = header.getUser();
        String username = (principal != null) ? principal.getName() : "anonymous";
        
        logger.info("📨 Received code update - SessionID: " + sessionId + ", Username: " + username + ", Code length: " + (code != null ? code.length() : 0));
        
        if (code != null && !code.trim().isEmpty()) {
            publisher.publishEvent(new CodeChangeEvent(this, username, code));
            logger.info("✓ CodeChangeEvent published for user: " + username);
        } else {
            logger.warn("⚠️ Received empty code");
        }
    }
}