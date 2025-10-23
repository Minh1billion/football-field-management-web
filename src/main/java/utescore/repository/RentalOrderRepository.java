package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utescore.entity.RentalOrder;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {
}
