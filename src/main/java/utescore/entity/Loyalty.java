package utescore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loyalties")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Loyalty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer points = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipTier tier = MembershipTier.BRONZE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalSpent = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer totalBookings = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", unique = true)
    @ToString.Exclude
    private Customer customer;

    public enum MembershipTier {
        BRONZE, SILVER, GOLD, PLATINUM
    }

    public void addPoints(Integer pointsToAdd) {
        this.points += pointsToAdd;
        updateTier();
    }

    private void updateTier() {
        if (points >= 10000) {
            tier = MembershipTier.PLATINUM;
        } else if (points >= 5000) {
            tier = MembershipTier.GOLD;
        } else if (points >= 2000) {
            tier = MembershipTier.SILVER;
        } else {
            tier = MembershipTier.BRONZE;
        }
    }
}