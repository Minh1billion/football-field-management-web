package utescore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
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
    private String content;

    @Column(columnDefinition = "NVARCHAR(500)")
    private String imageUrl;

    private int likes = 0;
    private LocalDateTime createdAt = LocalDateTime.now();

    // ✅ Thêm trạng thái phê duyệt
    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.PENDING; // Mặc định chờ duyệt

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments;
    
    public enum PostStatus {
        PENDING,   // Chờ duyệt
        APPROVED,  // Đã duyệt
        REJECTED   // Từ chối
    }
}
