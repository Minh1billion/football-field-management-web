package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utescore.entity.OrderItem;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    
    List<OrderItem> findByOrderId(Long orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findOrderItemsByOrderId(@Param("orderId") Long orderId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.service.id = :serviceId")
    List<OrderItem> findByServiceId(@Param("serviceId") Long serviceId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.sportWear.id = :sportWearId")
    List<OrderItem> findBySportWearId(@Param("sportWearId") Long sportWearId);
}