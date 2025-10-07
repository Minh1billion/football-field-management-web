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
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sport_wears")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SportWear {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WearType wearType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Size size;

    @Column(nullable = false)
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

    private String brand;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "sportWear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<BookingSportWear> bookingSportWears = new HashSet<>();

    @OneToMany(mappedBy = "sportWear", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<OrderItem> orderItems = new HashSet<>();

    public enum WearType {
        JERSEY, SHORTS, SOCKS, SHOES, SHIN_GUARDS, GLOVES, GOALKEEPER_KIT
    }

    public enum Size {
        XS, S, M, L, XL, XXL, XXXL
    }
}