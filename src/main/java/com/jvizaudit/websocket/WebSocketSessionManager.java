package com.jvizaudit.websocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionManager {
    @Autowired
    private SimpMessagingTemplate template;

    /**
     * Send message to authenticated user
     * @param username The authenticated username (NOT sessionId)
     * @param dest Destination (e.g., "/queue/uml" or "/queue/flowchart")
     * @param payload The message content
     */
    public void sendToSession(String username, String dest, Object payload) {
        // convertAndSendToUser automatically routes to /user/{username}/{dest}
        template.convertAndSendToUser(username, dest, payload);
    }
}