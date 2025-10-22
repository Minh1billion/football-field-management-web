package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utescore.entity.Notification;
import utescore.repository.NotificationRepository;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public long countUnread(String username) {
        return notificationRepository.countUnreadByUsername(username);
    }

    // Đánh dấu tất cả thông báo của user là đã đọc
    public void markAllAsRead(String username) {
        var notifications = notificationRepository.findAll().stream()
                .filter(n -> n.getAccount().getUsername().equals(username) && !n.getIsRead())
                .toList();

        notifications.forEach(Notification::markAsRead);
        notificationRepository.saveAll(notifications);
    }
}

