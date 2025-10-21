package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.entity.FootballField;
import utescore.entity.Location;
import utescore.service.FieldManagementService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/manager/fields")
@RequiredArgsConstructor
public class ManagerFieldController {

    private final FieldManagementService fieldService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("fields", fieldService.listAll());
        model.addAttribute("locations", fieldService.listLocations());
        return "manager/fields/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("field", new FootballField());
        model.addAttribute("locations", fieldService.listLocations());
        model.addAttribute("fieldTypes", utescore.entity.FootballField.FieldType.values());
        model.addAttribute("surfaceTypes", utescore.entity.FootballField.SurfaceType.values());
        return "manager/fields/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        FootballField field = fieldService.get(id).orElseThrow(() -> new IllegalArgumentException("Field not found"));
        model.addAttribute("field", field);
        model.addAttribute("locations", fieldService.listLocations());
        model.addAttribute("fieldTypes", utescore.entity.FootballField.FieldType.values());
        model.addAttribute("surfaceTypes", utescore.entity.FootballField.SurfaceType.values());
        return "manager/fields/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute FootballField field,
                       @RequestParam(required = false) Long locationId) {
        fieldService.save(field, locationId);
        return "redirect:/manager/fields";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        fieldService.delete(id);
        return "redirect:/manager/fields";
    }

    @GetMapping("/available")
    public String searchAvailable(@RequestParam(required = false) Long locationId,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
                                  Model model) {
        List<FootballField> avail = fieldService.findAvailableFields(locationId, start, end);
        model.addAttribute("availableFields", avail);
        model.addAttribute("locations", fieldService.listLocations());
        model.addAttribute("start", start);
        model.addAttribute("end", end);
        model.addAttribute("selectedLocationId", locationId);
        return "manager/fields/available";
    }
}