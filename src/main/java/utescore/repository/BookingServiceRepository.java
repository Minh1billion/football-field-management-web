package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import utescore.entity.BookingService;

@Repository
public interface BookingServiceRepository extends JpaRepository<BookingService, Long> {
    
    @Query("SELECT COUNT(bs) FROM BookingService bs WHERE bs.service.id = :serviceId")
    long countByServiceId(@Param("serviceId") Long serviceId);
    
    @Query("SELECT SUM(bs.quantity) FROM BookingService bs WHERE bs.service.id = :serviceId")
    Long sumQuantityByServiceId(@Param("serviceId") Long serviceId);
}