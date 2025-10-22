package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import utescore.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.account.username = :username AND n.isRead = false")
    long countUnreadByUsername(String username);
}
