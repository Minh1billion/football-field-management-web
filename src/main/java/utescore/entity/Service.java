package utescore.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
@Table(name = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tên không được để trống, tối thiểu 3 ký tự
    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(min = 3, max = 255, message = "Tên dịch vụ phải từ 3 đến 255 ký tự")
    @Column(nullable = false, columnDefinition = "NVARCHAR(255)")
    private String name;

    // Mô tả có thể trống, nhưng nếu có thì phải ít nhất 5 ký tự
    @Size(min = 5, message = "Mô tả phải có ít nhất 5 ký tự")
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // Enum không được null
    @NotNull(message = "Loại dịch vụ là bắt buộc")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType serviceType;

    // Giá phải lớn hơn 0
    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    @Digits(integer = 8, fraction = 2, message = "Giá không hợp lệ")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    private String imageUrl;

    @Column(nullable = false)
    private Integer stockQuantity = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<BookingService> bookingServices = new HashSet<>();

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private Set<OrderItem> orderItems = new HashSet<>();

    public enum ServiceType {
        FOOD, DRINK, EQUIPMENT, REFEREE, PHOTOGRAPHY, LIVESTREAM, OTHER
    }
}