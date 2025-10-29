package utescore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "NVARCHAR(MAX)",nullable = false)
    private String title;

    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @Column(columnDefinition = "TEXT")
    private String hiddenByUsers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @ToString.Exclude
    private Account account;

    public enum NotificationType {
        BOOKING_CONFIRMATION,
        BOOKING_REMINDER,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PROMOTION,
        MAINTENANCE_ALERT,
        SYSTEM_UPDATE,
        GENERAL
    }

    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }

    public boolean isHiddenBy(String username) {
        if (hiddenByUsers == null) return false;
        return hiddenByUsers.contains(username);
    }

    public void hideFor(String username) {
        if (hiddenByUsers == null) {
            hiddenByUsers = username;
        } else if (!isHiddenBy(username)) {
            hiddenByUsers += "," + username;
        }
    }

    public void unhideFor(String username) {
        if (hiddenByUsers == null) return;

        String[] users = hiddenByUsers.split(",");
        StringBuilder newList = new StringBuilder();

        for (String user : users) {
            if (!user.equals(username)) {
                if (newList.length() > 0) newList.append(",");
                newList.append(user);
            }
        }

        hiddenByUsers = newList.length() > 0 ? newList.toString() : null;
    }
}