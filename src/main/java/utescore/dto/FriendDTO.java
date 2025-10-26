package utescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendDTO {
    private Long accountId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private boolean isFriend;
    private Integer mutualFriendsCount;
    private String requestStatus; // SENT, RECEIVED, null
    private Long requestId;
}
