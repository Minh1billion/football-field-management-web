package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.Notification;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByCreatedAtDesc();
    List<Notification> findByAccount_IdOrderByCreatedAtDesc(Long accountId);
}