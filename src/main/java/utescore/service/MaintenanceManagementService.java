package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.FootballField;
import utescore.entity.Maintenance;
import utescore.repository.FootballFieldRepository;
import utescore.repository.MaintenanceRepository;

import java.time.LocalDateTime;
import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceManagementService {

    private final MaintenanceRepository maintenanceRepo;
    private final FootballFieldRepository fieldRepo;

    public List<Maintenance> listAll() {
        return maintenanceRepo.findAll();
    }

    public Maintenance schedule(Long fieldId,
                                String title,
                                String description,
                                Maintenance.MaintenanceType type,
                                LocalDateTime scheduledDate,
                                String performedBy) {
        FootballField field = fieldRepo.findById(fieldId).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        Maintenance m = new Maintenance();
        m.setField(field);
        m.setTitle(title);
        m.setDescription(description);
        m.setType(type);
        m.setScheduledDate(scheduledDate);
        m.setPerformedBy(performedBy);
        m.setStatus(Maintenance.MaintenanceStatus.SCHEDULED);
        return maintenanceRepo.save(m);
    }

    public Maintenance updateStatus(Long id, Maintenance.MaintenanceStatus status) {
        Maintenance m = maintenanceRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Maintenance not found"));
        m.setStatus(status);
        if (status == Maintenance.MaintenanceStatus.COMPLETED) {
            m.setCompletedDate(java.time.LocalDateTime.now());
        }
        return maintenanceRepo.save(m);
    }

    public void delete(Long id) {
        maintenanceRepo.deleteById(id);
    }
}