package utescore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String author;  // usernamelogin

    @Column(columnDefinition = "NVARCHAR(MAX)")
    @NotBlank(message = "Nội dung không được để trống")
    @Size(min = 3, message = "Nội dung phải có ít nhất 3 ký tự")
    private String content;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String imageUrl;

    private int likes = 0;

    private LocalDateTime createdAt;

    // ✅ Thêm trạng thái phê duyệt
    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    // ✅ Danh sách username đã like (JSON hoặc bảng riêng)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_likes", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "username")
    @Builder.Default
    private List<String> likedByUsers = new ArrayList<>();

    public enum PostStatus {
        PENDING,   // Chờ duyệt
        APPROVED,  // Đã duyệt
        REJECTED   // Từ chối
    }

    /**
     * ✅ Tự động set giá trị mặc định khi tạo mới
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = PostStatus.PENDING;
        }
        if (likedByUsers == null) {
            likedByUsers = new ArrayList<>();
        }
    }

    /**
     * ✅ Kiểm tra user đã like chưa
     */
    public boolean isLikedBy(String username) {
        return likedByUsers != null && likedByUsers.contains(username);
    }

    /**
     * ✅ Thêm like
     */
    public boolean addLike(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        if (likedByUsers == null) {
            likedByUsers = new ArrayList<>();
        }
        if (!likedByUsers.contains(username)) {
            likedByUsers.add(username);
            likes = likedByUsers.size();
            return true;
        }
        return false;
    }

    /**
     * ✅ Bỏ like
     */
    public boolean removeLike(String username) {
        if (likedByUsers != null && likedByUsers.remove(username)) {
            likes = likedByUsers.size();
            return true;
        }
        return false;
    }
}