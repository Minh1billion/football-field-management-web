package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.FootballFieldDTO;
import utescore.dto.LocationDTO;
import utescore.entity.Account;
import utescore.entity.FootballField;
import utescore.entity.Location;
import utescore.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class FieldManagementService {

    private final FootballFieldRepository fieldRepo;
    private final LocationRepository locationRepo;
    private final BookingRepository bookingRepo;
    private final FieldAvailabilityRepository availabilityRepo;
    private final MaintenanceRepository maintenanceRepo;
    private final AccountRepository accountRepo;

    public List<FootballFieldDTO> listAll() {
        List<FootballFieldDTO> fields = new ArrayList<>();
        fieldRepo.findAll().forEach(f -> fields.add(convertToDTO(f)));
        return fields;
    }

    public FootballFieldDTO getFieldById(Long id) {
        fieldRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        return convertToDTO(fieldRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Field not found")));
    }

    public Optional<FootballField> get(Long id) {
        return fieldRepo.findById(id);
    }

    public FootballField save(FootballField field, Long locationId) {
        if (locationId != null) {
            Location loc = locationRepo.findById(locationId).orElse(null);
            field.setLocation(loc);
        }
        return fieldRepo.save(field);
    }

    public void delete(Long id) {
        fieldRepo.deleteById(id);
    }

    public List<Location> listLocations() {
        return locationRepo.findAll();
    }

    public List<FootballFieldDTO> findAvailableFields(Long locationId, LocalDateTime start, LocalDateTime end) {
        List<FootballField> base = (locationId != null)
                ? fieldRepo.findByLocation_Id(locationId)
                : fieldRepo.findByIsActiveTrue();

        List<FootballField> available = new ArrayList<>();

        for (FootballField f : base) {
            boolean overlapBooking = bookingRepo.existsOverlap(f.getId(), start, end);
            boolean blockedByAvailability = !availabilityRepo.findBlockingWindows(f.getId(), start, end).isEmpty();
            boolean plannedMaintenance = !maintenanceRepo.findPlannedInRange(f.getId(), start, end).isEmpty();

            if (!overlapBooking && !blockedByAvailability && !plannedMaintenance && Boolean.TRUE.equals(f.getIsActive())) {
                available.add(f);
            }
        }

        List<FootballFieldDTO> availableDto = new ArrayList<>();
        available.forEach(f -> availableDto.add(convertToDTO(f)));
        return availableDto;
    }

    public List<LocationDTO> getAllLocations() {
        List<Location> locations = locationRepo.findAll();
        List<LocationDTO> locationDto = new ArrayList<>();
        locations.forEach(l -> locationDto.add(convertToDTO(l)));
        return locationDto;
    }

    public long countFieldsByManager(String username) {
        Account manager = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        return fieldRepo.countByManagerId(manager.getId());
    }

    public long countActiveFieldsByManager(String username) {
        Account manager = accountRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        return bookingRepo.countActiveFieldsByManagerId(manager.getId(), LocalDateTime.now());
    }

    private FootballFieldDTO convertToDTO(FootballField f) {
        FootballFieldDTO dto = new FootballFieldDTO();
        dto.setId(f.getId());
        dto.setName(f.getName());
        dto.setFieldType(f.getFieldType().toString());
        dto.setCapacity(f.getCapacity());
        dto.setSurfaceType(f.getSurfaceType().toString());
        dto.setPricePerHour(f.getPricePerHour());
        dto.setDescription(f.getDescription());
        dto.setIsActive(f.getIsActive());
        dto.setLocationId(f.getLocation() != null ? f.getLocation().getId() : null);
        dto.setLocationName(f.getLocation() != null ? f.getLocation().getName() : "Unknown location");
        dto.setLocationAddress(f.getLocation() != null ? f.getLocation().getAddress() : "Unknown address");
        return dto;
    }

    private LocationDTO convertToDTO(Location l) {
        LocationDTO dto = new LocationDTO();
        dto.setId(l.getId());
        dto.setAddress(l.getAddress());
        dto.setName(l.getName());
        return dto;
    }
}