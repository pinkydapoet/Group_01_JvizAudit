package com.jvizaudit.websocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketSessionManager {
    @Autowired
    private SimpMessagingTemplate template;

    public void sendToSession(String username, String dest, Object payload) {
        template.convertAndSendToUser(username, dest, payload);
    }
}