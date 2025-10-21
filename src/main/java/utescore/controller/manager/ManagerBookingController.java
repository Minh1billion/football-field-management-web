package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.dto.BookingRequestDTO;
import utescore.entity.Booking;
import utescore.service.BookingManagementService;
import utescore.service.FieldManagementService;
import utescore.repository.CustomerRepository;
import utescore.repository.ServiceRepository;
import utescore.repository.SportWearRepository;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/manager/bookings")
@RequiredArgsConstructor
public class ManagerBookingController {

    private final BookingManagementService bookingService;
    private final FieldManagementService fieldService;
    private final CustomerRepository customerRepo;
    private final ServiceRepository serviceRepo;
    private final SportWearRepository sportWearRepo;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bookings", bookingService.listAll());
        return "manager/bookings/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("fields", fieldService.listAll());
        model.addAttribute("customers", customerRepo.findAll());
        model.addAttribute("services", serviceRepo.findAll());
        model.addAttribute("sportWears", sportWearRepo.findAll());
        return "manager/bookings/form";
    }

    @PostMapping("/create")
    public String create(@RequestParam Long fieldId,
                         @RequestParam Long customerId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
                         @RequestParam(required = false) java.util.List<Long> serviceIds,
                         @RequestParam(required = false) java.util.List<Long> sportWearIds,
                         Model model) {
        try {
            BookingRequestDTO req = new BookingRequestDTO();
            req.setFieldId(fieldId);
            req.setCustomerId(customerId);
            req.setStartTime(startTime);
            req.setEndTime(endTime);
            req.setServiceIds(serviceIds);
            req.setSportWearIds(sportWearIds);

            bookingService.createBooking(req);
            return "redirect:/manager/bookings";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return newForm(model);
        }
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id) {
        bookingService.confirm(id);
        return "redirect:/manager/bookings";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id) {
        bookingService.cancel(id);
        return "redirect:/manager/bookings";
    }
}