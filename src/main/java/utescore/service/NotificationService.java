package utescore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.NotificationDTO;
import utescore.entity.Account;
import utescore.entity.Notification;
import utescore.repository.AccountRepository;
import utescore.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Admin: Xem TẤT CẢ notification
    @Transactional(readOnly = true)
    public List<Notification> listAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    // User: Chỉ xem notification chưa bị ẩn
    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByUsername(String username) {
        return notificationRepository.findByAccountUsernameOrderByCreatedAtDesc(username);
    }

    public void sendToAccount(Long accountId, String title, String message, Notification.NotificationType type) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Notification n = new Notification();
        n.setAccount(acc);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        notificationRepository.save(n);

        log.info("Notification sent to user {}: {} - {}", acc.getUsername(), title, type);

        NotificationDTO dto = NotificationDTO.fromEntity(n);
        messagingTemplate.convertAndSendToUser(
                acc.getUsername(),
                "/topic/notifications",
                dto
        );
    }

    public int sendToAllUsers(String title, String message, Notification.NotificationType type) {
        List<Account> users = accountRepository.findByRole(Account.Role.USER);

        for (Account acc : users) {
            Notification n = new Notification();
            n.setAccount(acc);
            n.setTitle(title);
            n.setMessage(message);
            n.setType(type);
            notificationRepository.save(n);

            NotificationDTO dto = NotificationDTO.fromEntity(n);
            messagingTemplate.convertAndSendToUser(
                    acc.getUsername(),
                    "/topic/notifications",
                    dto
            );
        }

        log.info("Broadcast notification sent to {} users: {} - {}", users.size(), title, type);
        return users.size();
    }

    public long countUnread(String username) {
        return notificationRepository.countUnreadByUsername(username);
    }

    public void markAsRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getAccount().getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        notification.markAsRead();
        notificationRepository.save(notification);

        log.debug("Notification {} marked as read by {}", notificationId, username);
    }

    public void markAllAsRead(String username) {
        List<Notification> notifications = notificationRepository
                .findByAccountUsernameAndIsReadFalse(username);

        for (Notification n : notifications) {
            n.markAsRead();
        }
        notificationRepository.saveAll(notifications);

        log.info("Marked {} notifications as read for user {}", notifications.size(), username);
    }

    /**
     * Ẩn notification - CHỈ ẨN với user hiện tại, không ảnh hưởng đến admin/user khác
     */
    public void deleteNotification(Long notificationId, String username) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getAccount().getUsername().equals(username)) {
            throw new IllegalArgumentException("Not authorized");
        }

        // Ẩn notification cho user này
        notification.hideFor(username);
        notificationRepository.save(notification);

        log.info("Notification {} hidden by user {}", notificationId, username);
    }

    /**
     * Hard delete - CHỈ ADMIN mới được xóa vĩnh viễn
     */
    public void hardDelete(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        notificationRepository.delete(notification);

        log.warn("Notification {} permanently deleted", notificationId);
    }

    /**
     * Lấy danh sách notification đã ẩn (cho tính năng restore)
     */
    @Transactional(readOnly = true)
    public List<Notification> getHiddenNotifications(String username) {
        return notificationRepository.findHiddenByUsername(username);
    }

    /**
     * Cleanup job: Xóa vĩnh viễn notification quá cũ (chạy định kỳ)
     * Điều kiện: Tất cả user đều đã ẩn hoặc notification cũ hơn 6 tháng
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        LocalDateTime threshold = LocalDateTime.now().minusMonths(6);

        // Có thể thêm logic phức tạp hơn ở đây
        log.info("Running scheduled cleanup for notifications older than {}", threshold);
    }

    @Transactional(readOnly = true)
    public Object getStatistics(LocalDateTime from) {
        return notificationRepository.getStatisticsSince(from);
    }
}