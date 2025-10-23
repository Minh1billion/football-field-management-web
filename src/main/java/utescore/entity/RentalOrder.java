package utescore.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "rental_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "account_id")
    private Account account;

    private String customerName;
    private String customerPhone;

    @Column(columnDefinition = "TEXT")
    private String customerAddress;

    @OneToOne(mappedBy = "rentalOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Payment payment;

    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "rentalOrder", cascade = CascadeType.ALL)
    private List<RentalOrderDetail> orderDetails;
}
