package utescore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "sport_wears")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"bookingSportWears", "orderItems"})
public class SportWear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String name;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WearType wearType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Size size;

    @Column(nullable = false, columnDefinition = "NVARCHAR(100)")
    private String color;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rentalPricePerDay;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sellPrice;

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    private Boolean isAvailableForRent = true;

    @Column(nullable = false)
    private Boolean isAvailableForSale = true;

    private String imageUrl;

    @Column(columnDefinition = "NVARCHAR(255)")
    private String brand;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "sportWear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BookingSportWear> bookingSportWears = new HashSet<>();

    @OneToMany(mappedBy = "sportWear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<OrderItem> orderItems = new HashSet<>();

    public enum WearType {
        JERSEY, SHORTS, SOCKS, SHOES, SHIN_GUARDS, GLOVES, GOALKEEPER_KIT
    }

    public enum Size {
        XS, S, M, L, XL, XXL, XXXL
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SportWear)) return false;
        SportWear that = (SportWear) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
