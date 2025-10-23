package utescore.util;

import lombok.RequiredArgsConstructor;
import utescore.service.ActiveUserService;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final ActiveUserService activeUserService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getFirstNativeHeader("username");
        if (username != null) {
            accessor.getSessionAttributes().put("username", username);
            activeUserService.addUser(username);
            System.out.println("User connected: " + username + ", Active users: "+ username);
            messagingTemplate.convertAndSend("/topic/activeUsers", activeUserService.getActiveUsers());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getSessionAttributes() != null
                ? (String) accessor.getSessionAttributes().get("username")
                : null;
        if (username != null) {
            activeUserService.removeUser(username);
            messagingTemplate.convertAndSend("/topic/activeUsers", activeUserService.getActiveUsers());
        }
    }
}

