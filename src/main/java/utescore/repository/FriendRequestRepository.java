package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utescore.entity.Account;
import utescore.entity.FriendRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    // Tìm lời mời kết bạn đang pending giữa 2 người
    Optional<FriendRequest> findBySenderAndReceiverAndStatus(
            Account sender,
            Account receiver,
            FriendRequest.Status status
    );

    // Kiểm tra xem đã có lời mời giữa 2 người chưa (bất kể chiều nào)
    @Query("SELECT fr FROM FriendRequest fr WHERE " +
            "((fr.sender = :user1 AND fr.receiver = :user2) OR " +
            "(fr.sender = :user2 AND fr.receiver = :user1)) AND " +
            "fr.status = :status")
    Optional<FriendRequest> findPendingRequestBetween(
            @Param("user1") Account user1,
            @Param("user2") Account user2,
            @Param("status") FriendRequest.Status status
    );

    // Lấy danh sách lời mời đã nhận (pending)
    List<FriendRequest> findByReceiverAndStatus(
            Account receiver,
            FriendRequest.Status status
    );

    // Lấy danh sách lời mời đã gửi (pending)
    List<FriendRequest> findBySenderAndStatus(
            Account sender,
            FriendRequest.Status status
    );

    // Đếm số lời mời đang chờ
    long countByReceiverAndStatus(Account receiver, FriendRequest.Status status);
}