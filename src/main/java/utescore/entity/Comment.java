package utescore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên người bình luận
    @Column(columnDefinition = "NVARCHAR(255)")
    private String author;

    // Nội dung bình luận
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;

    private LocalDateTime createdAt = LocalDateTime.now();

    // Liên kết đến bài post
    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;
}