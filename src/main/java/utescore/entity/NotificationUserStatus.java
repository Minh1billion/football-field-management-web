package utescore.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_user_status")
public class NotificationUserStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private Boolean isHidden = false;  // User đã ẩn notification này
    private Boolean isRead = false;
    private LocalDateTime readAt;
    private LocalDateTime hiddenAt;
}
