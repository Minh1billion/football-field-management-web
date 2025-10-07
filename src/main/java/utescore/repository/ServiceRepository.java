package utescore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import utescore.entity.Service;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {
    
    Page<Service> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<Service> findByServiceType(Service.ServiceType serviceType, Pageable pageable);
    
    Page<Service> findByNameContainingIgnoreCaseAndServiceType(
            String name, Service.ServiceType serviceType, Pageable pageable);
    
    List<Service> findByIsAvailableTrue();
    
    List<Service> findByServiceType(Service.ServiceType serviceType);
    
    List<Service> findByServiceTypeAndIsAvailableTrue(Service.ServiceType serviceType);
}