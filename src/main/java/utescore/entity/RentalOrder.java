package utescore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_address", columnDefinition = "TEXT")
    private String customerAddress;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "payment_method")
    private String paymentMethod; // COD, VNPAY

    @Column(name = "payment_status")
    private String paymentStatus; // PAID, UNPAID

    @Column(name = "order_status")
    private String orderStatus; // PENDING, CONFIRMED, SHIPPING, DELIVERED, CANCELLED

    @Column(name = "order_date")
    private LocalDateTime orderDate;

    @OneToMany(mappedBy = "rentalOrder", cascade = CascadeType.ALL)
    private List<RentalOrderDetail> orderDetails;
}
