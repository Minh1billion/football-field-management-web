package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.entity.Maintenance;
import utescore.service.FieldManagementService;
import utescore.service.MaintenanceManagementService;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/manager/maintenances")
@RequiredArgsConstructor
public class ManagerMaintenanceController {

    private final MaintenanceManagementService maintenanceService;
    private final FieldManagementService fieldService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("maintenances", maintenanceService.listAll());
        model.addAttribute("fields", fieldService.listAll());
        model.addAttribute("types", Maintenance.MaintenanceType.values());
        model.addAttribute("statuses", Maintenance.MaintenanceStatus.values());
        return "manager/maintenances/list";
    }

    @PostMapping("/schedule")
    public String schedule(@RequestParam Long fieldId,
                           @RequestParam String title,
                           @RequestParam(required = false) String description,
                           @RequestParam Maintenance.MaintenanceType type,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledDate,
                           @RequestParam(defaultValue = "2") Integer estimatedDurationHours, // ✅ Thêm param
                           @RequestParam(required = false) String performedBy) {
        maintenanceService.schedule(fieldId, title, description, type, scheduledDate,
                estimatedDurationHours, performedBy);
        return "redirect:/manager/maintenances";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam Maintenance.MaintenanceStatus status) {
        maintenanceService.updateStatus(id, status);
        return "redirect:/manager/maintenances";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        maintenanceService.delete(id);
        return "redirect:/manager/maintenances";
    }
}