package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.Account;
import utescore.entity.FootballField;
import utescore.entity.Maintenance;
import utescore.repository.AccountRepository;
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
    private final AccountRepository accountRepo;

    public List<Maintenance> listAll() {
        return maintenanceRepo.findAll();
    }

    public Maintenance schedule(Long fieldId,
                                String title,
                                String description,
                                Maintenance.MaintenanceType type,
                                LocalDateTime scheduledDate,
                                Integer estimatedDurationHours, 
                                String performedBy) {
        FootballField field = fieldRepo.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Field not found"));

        Maintenance m = new Maintenance();
        m.setField(field);
        m.setTitle(title);
        m.setDescription(description);
        m.setType(type);
        m.setScheduledDate(scheduledDate);
        m.setEstimatedDurationHours(estimatedDurationHours != null ? estimatedDurationHours : 2);
        m.setPerformedBy(performedBy);
        m.setStatus(Maintenance.MaintenanceStatus.SCHEDULED);

        Maintenance saved = maintenanceRepo.save(m);

        LocalDateTime now = LocalDateTime.now();
        if (scheduledDate.isBefore(now.plusHours(1)) && Boolean.TRUE.equals(field.getIsActive())) {
            field.setIsActive(false);
            fieldRepo.save(field);
        }

        return saved;
    }

    public Maintenance updateStatus(Long id, Maintenance.MaintenanceStatus status) {
        Maintenance m = maintenanceRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Maintenance not found"));
        m.setStatus(status);
        if (status == Maintenance.MaintenanceStatus.COMPLETED) {
            m.setCompletedDate(java.time.LocalDateTime.now());
        }
        Maintenance updated = maintenanceRepo.save(m);

        FootballField field = m.getField();
        if (field != null) {
            switch (status) {
                case SCHEDULED, IN_PROGRESS -> {
                    // Đảm bảo sân đang tạm ngừng trong thời gian bảo trì
                    if (Boolean.TRUE.equals(field.getIsActive())) {
                        field.setIsActive(false);
                        fieldRepo.save(field);
                    }
                }
                case COMPLETED, CANCELLED -> {
                    // Bật lại sân nếu không còn maintenance mở nào khác
                    long openCount = maintenanceRepo.countByField_IdAndStatusIn(
                            field.getId(),
                            java.util.List.of(Maintenance.MaintenanceStatus.SCHEDULED, Maintenance.MaintenanceStatus.IN_PROGRESS)
                    );
                    if (openCount == 0 && Boolean.FALSE.equals(field.getIsActive())) {
                        field.setIsActive(true);
                        fieldRepo.save(field);
                    }
                }
            }
        }

        return updated;
    }

    public long countUpcomingMaintenanceByManager(String username, LocalDateTime now, LocalDateTime end) {
        Account manager = accountRepo.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("Manager not found"));
        return maintenanceRepo.countByField_ManagerIdAndScheduledDateBetween(manager.getId(), now, end);
    }

    public long countActiveMaintenanceFieldsByManager(String username) {
        Account manager = accountRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("Manager not found"));
        return maintenanceRepo.countActiveMaintenanceFieldsByManagerUsername(manager.getId());
    }

    public void delete(Long id) {
        Maintenance m = maintenanceRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Maintenance not found"));
        FootballField field = m.getField();

        maintenanceRepo.deleteById(id);

        // Sau khi xóa, nếu không còn maintenance mở thì bật lại sân
        if (field != null) {
            long openCount = maintenanceRepo.countByField_IdAndStatusIn(
                    field.getId(),
                    java.util.List.of(Maintenance.MaintenanceStatus.SCHEDULED, Maintenance.MaintenanceStatus.IN_PROGRESS)
            );
            if (openCount == 0 && Boolean.FALSE.equals(field.getIsActive())) {
                field.setIsActive(true);
                fieldRepo.save(field);
            }
        }
    }
}