package utescore.util;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Map<String, Boolean> activeUsers = new ConcurrentHashMap<>();
    private static final Map<String, LocalDateTime> activityTimes = new ConcurrentHashMap<>();

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = accessor.getFirstNativeHeader("username");

        if (username != null && !username.isEmpty()) {
        		if (!Boolean.TRUE.equals(activeUsers.get(username))) {
                activityTimes.put(username, LocalDateTime.now());
            }
            activeUsers.put(username, true);
            accessor.getSessionAttributes().put("username", username);
        }
        sendActiveUsersUpdate();
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String username = (String) accessor.getSessionAttributes().get("username");

        if (username != null) {
            activeUsers.put(username, false);
            activityTimes.put(username, LocalDateTime.now());
        }
        sendActiveUsersUpdate();
    }

    private void sendActiveUsersUpdate() {
        Map<String, Map<String, Object>> data = new ConcurrentHashMap<>();
        for (String user : activeUsers.keySet()) {
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("online", activeUsers.get(user));
            info.put("time", activityTimes.get(user));
            data.put(user, info);
        }
        messagingTemplate.convertAndSend("/topic/activeUsers", data);
    }

    public static Map<String, Map<String, Object>> getActiveUsersWithTime() {
        Map<String, Map<String, Object>> data = new ConcurrentHashMap<>();
        for (String user : activeUsers.keySet()) {
            Map<String, Object> info = new ConcurrentHashMap<>();
            info.put("online", activeUsers.get(user));
            info.put("time", activityTimes.get(user));
            data.put(user, info);
        }
        return data;
    }
}
