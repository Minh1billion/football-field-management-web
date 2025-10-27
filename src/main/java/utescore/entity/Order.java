package utescore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"payment", "account", "customer", "orderItems"})
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @ToString.Exclude
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @ToString.Exclude
    private Customer customer;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String customerName;

    @Column(columnDefinition = "NVARCHAR(20)")
    private String customerPhone;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String customerEmail;

    @Column(columnDefinition = "NVARCHAR(100)")
    private String customerCity;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String customerAddress;

    @Column(precision = 10, scale = 2)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<OrderItem> orderItems = new HashSet<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Payment payment;

    // Enum trạng thái đơn hàng
    public enum OrderStatus {
        PENDING,      // Chờ xác nhận
        PROCESSING,   // Đang xử lý
        READY,        // Sẵn sàng giao
        SHIPPING,     // Đang giao hàng
        DELIVERED,    // Đã giao hàng
        COMPLETED,    // Hoàn thành
        CANCELLED     // Đã hủy
    }
}
