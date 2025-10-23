package utescore.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rental_order_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RentalOrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rental_order_id")
    private RentalOrder rentalOrder;

    @ManyToOne
    @JoinColumn(name = "sport_wear_id")
    private SportWear sportWear;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "rental_days")
    private Integer rentalDays;

    @Column(name = "rental_price_per_day")
    private Double rentalPricePerDay;

    @Column(name = "sub_total")
    private Double subTotal;
}