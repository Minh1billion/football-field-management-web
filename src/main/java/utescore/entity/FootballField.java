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
@Table(name = "football_fields")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FootballField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FieldType fieldType;

    @Column(nullable = false)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SurfaceType surfaceType;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean hasLighting = false;

    @Column(nullable = false)
    private Boolean hasChangingRoom = false;

    @Column(nullable = false)
    private Boolean hasParking = false;

    private String imageUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    @ToString.Exclude
    private Location location;

    private Long managerId;

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Booking> bookings = new HashSet<>();

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Review> reviews = new HashSet<>();

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<Maintenance> maintenances = new HashSet<>();

    @OneToMany(mappedBy = "field", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<FieldAvailability> availabilities = new HashSet<>();

    public enum FieldType {
        FIELD_5V5, FIELD_7V7, FIELD_11V11, FUTSAL
    }

    public enum SurfaceType {
        NATURAL_GRASS, ARTIFICIAL_TURF, INDOOR
    }
}