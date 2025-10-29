package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utescore.entity.Account;
import utescore.entity.Notification;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Lấy tất cả notification của user CHƯA BỊ ẨN
     * Query này filter ra những notification mà user đã ẩn
     */
    @Query("SELECT n FROM Notification n WHERE n.account.username = :username " +
            "AND (n.hiddenByUsers IS NULL OR n.hiddenByUsers NOT LIKE CONCAT('%', :username, '%')) " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findByAccountUsernameOrderByCreatedAtDesc(@Param("username") String username);

    /**
     * Đếm số notification chưa đọc VÀ chưa bị ẩn của user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.account.username = :username " +
            "AND n.isRead = false " +
            "AND (n.hiddenByUsers IS NULL OR n.hiddenByUsers NOT LIKE CONCAT('%', :username, '%'))")
    long countUnreadByUsername(@Param("username") String username);

    /**
     * Lấy các notification chưa đọc VÀ chưa bị ẩn (dùng cho markAllAsRead)
     */
    @Query("SELECT n FROM Notification n WHERE n.account.username = :username " +
            "AND n.isRead = false " +
            "AND (n.hiddenByUsers IS NULL OR n.hiddenByUsers NOT LIKE CONCAT('%', :username, '%'))")
    List<Notification> findByAccountUsernameAndIsReadFalse(@Param("username") String username);

    /**
     * Lấy danh sách notification ĐÃ ẨN của user (cho tính năng restore)
     */
    @Query("SELECT n FROM Notification n WHERE n.account.username = :username " +
            "AND n.hiddenByUsers LIKE CONCAT('%', :username, '%') " +
            "ORDER BY n.createdAt DESC")
    List<Notification> findHiddenByUsername(@Param("username") String username);

    /**
     * Admin: Lấy TẤT CẢ notification (không filter hidden)
     */
    List<Notification> findAllByOrderByCreatedAtDesc();

    /**
     * Tìm theo role của account (cho admin dashboard hoặc broadcast)
     */
    @Query("SELECT n FROM Notification n JOIN n.account a WHERE a.role = :role")
    List<Notification> findByAccountRole(@Param("role") Account.Role role);

    /**
     * Thống kê số lượng notification theo type kể từ một thời điểm
     */
    @Query("SELECT n.type as type, COUNT(n) as count FROM Notification n " +
            "WHERE n.createdAt >= :since GROUP BY n.type")
    List<Object[]> getStatisticsSince(@Param("since") LocalDateTime since);

    /**
     * Tìm notification cũ hơn threshold (dùng cho cleanup job)
     */
    @Query("SELECT n FROM Notification n WHERE n.createdAt < :threshold")
    List<Notification> findOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Đếm tổng số notification của một user (kể cả đã ẩn)
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.account.username = :username")
    long countTotalByUsername(@Param("username") String username);
}