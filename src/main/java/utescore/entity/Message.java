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

    private String sender;     // Tên người gửi
    private String receiver;   // Tên người nhận (admin hoặc user cụ thể)
    private String content;    // Nội dung tin nhắn
    private boolean recalled;  // Tin nhắn đã thu hồi chưa
    private LocalDateTime time = LocalDateTime.now();
}
