package utescore.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FriendRequestDTO {
    private Long id;
    private Long senderId;
    private String senderUsername;
    private String senderFullName;
    private String senderAvatarUrl;
    private Long receiverId;
    private String receiverUsername;
    private String receiverFullName;
    private String receiverAvatarUrl;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private Integer mutualFriendsCount;
}