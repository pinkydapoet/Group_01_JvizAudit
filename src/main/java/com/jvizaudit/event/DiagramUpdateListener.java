package com.jvizaudit.event;
import com.jvizaudit.service.facade.DiagramFacadeService;
import com.jvizaudit.websocket.WebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DiagramUpdateListener {
    private static final Logger logger = LoggerFactory.getLogger(DiagramUpdateListener.class);
    
    @Autowired
    private DiagramFacadeService facade;
    @Autowired
    private WebSocketSessionManager sessionManager;

    @Async
    @EventListener
    public void handleCodeChange(CodeChangeEvent event) {
        try {
            logger.info("🔄 Starting diagram generation for user: " + event.getSessionId());
            
            String uml = facade.generateDiagram(event.getSourceCode(), "UML");
            logger.info("✓ UML diagram generated, length: " + uml.length());
            sessionManager.sendToSession(event.getSessionId(), "/queue/uml", uml);
            logger.info("📤 UML sent to /user/" + event.getSessionId() + "/queue/uml");
            
            String fc = facade.generateDiagram(event.getSourceCode(), "FLOWCHART");
            logger.info("✓ Flowchart generated, length: " + fc.length());
            sessionManager.sendToSession(event.getSessionId(), "/queue/flowchart", fc);
            logger.info("📤 Flowchart sent to /user/" + event.getSessionId() + "/queue/flowchart");
            
            logger.info("✓✓ All diagrams sent successfully");
        } catch (Exception e) {
            logger.error("✗ Error generating diagrams", e);
        }
    }
}