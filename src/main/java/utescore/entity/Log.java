package utescore.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String action;
    private LocalDateTime createdAt;
    private LocalDateTime endDateTime;
    private String type = "SYSTEM";

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;
}
