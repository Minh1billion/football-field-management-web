package utescore.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String sender;     // Tên người gửi

    @Column(columnDefinition = "NVARCHAR(255)")
    private String receiver;   // Tên người nhận (admin hoặc user cụ thể)

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String content;    // Nội dung tin nhắn
    
    private boolean recalled;  // Tin nhắn đã thu hồi chưa
    private LocalDateTime time = LocalDateTime.now();
}
