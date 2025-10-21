package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public List<FootballField> listAll() {
        return fieldRepo.findAll();
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

    public List<FootballField> findAvailableFields(Long locationId, LocalDateTime start, LocalDateTime end) {
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
        return available;
    }
}