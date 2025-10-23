package utescore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import utescore.dto.ServiceDTO;
import utescore.repository.ServiceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ServiceService {

    @Autowired
    private ServiceRepository serviceRepository;

    public Page<utescore.entity.Service> findAll(Pageable pageable) {
        return serviceRepository.findAll(pageable);
    }

    public Page<utescore.entity.Service> findByNameContainingAndServiceType(
            String name, utescore.entity.Service.ServiceType serviceType, Pageable pageable) {
        if (name != null && !name.isEmpty() && serviceType != null) {
            return serviceRepository.findByNameContainingIgnoreCaseAndServiceType(name, serviceType, pageable);
        } else if (name != null && !name.isEmpty()) {
            return serviceRepository.findByNameContainingIgnoreCase(name, pageable);
        } else if (serviceType != null) {
            return serviceRepository.findByServiceType(serviceType, pageable);
        } else {
            return serviceRepository.findAll(pageable);
        }
    }

    public List<utescore.entity.Service> findAll() {
        return serviceRepository.findAll();
    }

    public utescore.entity.Service findById(Long id) {
        Optional<utescore.entity.Service> service = serviceRepository.findById(id);
        return service.orElse(null);
    }

    public utescore.entity.Service save(utescore.entity.Service service) {
        return serviceRepository.save(service);
    }

    public utescore.entity.Service update(utescore.entity.Service service) {
        return serviceRepository.save(service);
    }

    public void deleteById(Long id) {
        serviceRepository.deleteById(id);
    }

    public long countAllServices() {
        return serviceRepository.count();
    }

    public List<ServiceDTO> findAllAvailableServices() {
        List<utescore.entity.Service> services = serviceRepository.findByIsAvailableTrue();
        return services.stream().map(this::convertToDTO).toList();
    }

    public ServiceDTO convertToDTO(utescore.entity.Service service) {
        ServiceDTO dto = new ServiceDTO();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setPrice(service.getPrice());
        dto.setServiceType(service.getServiceType().toString());
        dto.setIsAvailable(true);
        return dto;
    }
}