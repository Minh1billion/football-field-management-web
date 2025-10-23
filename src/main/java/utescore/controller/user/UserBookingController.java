package utescore.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.*;
import utescore.service.BookingService;
import utescore.service.FieldManagementService;
import utescore.service.ServiceService;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/user/bookings")
@RequiredArgsConstructor
public class UserBookingController {

    private final BookingService bookingService;
    private final FieldManagementService fieldService;
    private final ServiceService serviceService;

    @GetMapping
    public String listBookings(Model model, Authentication auth) {
        String username = auth.getName();
        List<BookingDTO> bookings = bookingService.getBookingsByUsername(username);
        long upcomingCount = bookingService.countUpcomingBookings(username);
        long monthlySpending = bookingService.calculateMonthlySpending(username);

        model.addAttribute("bookings", bookings);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("monthlySpending", monthlySpending);
        return "user/bookings/list";
    }

    @GetMapping("/new")
    public String showBookingForm(@RequestParam(required = false) Long locationId,
                                  @RequestParam(required = false) Long fieldId,
                                  @RequestParam(required = false) String date,
                                  Model model) {
        LocalDate selectedDate;

        if (date == null || date.equals("null") || date.isBlank()) {
            selectedDate = LocalDate.now();
        } else {
            selectedDate = LocalDate.parse(date);
        }

        List<LocationDTO> locations = fieldService.getAllLocations();
        List<FootballFieldDTO> allFields = fieldService.listAll();

        // Lọc fields theo location nếu có
        List<FootballFieldDTO> fields = allFields;
        if (locationId != null) {
            fields = allFields.stream()
                    .filter(f -> f.getLocationId().equals(locationId))
                    .toList();
        }

        // Load time slots nếu đã chọn field và date
        List<TimeSlotDTO> timeSlots = null;
        FootballFieldDTO selectedField = null;
        if (fieldId != null && date != null) {
            timeSlots = bookingService.getAvailableTimeSlots(fieldId, selectedDate);
            selectedField = fieldService.getFieldById(fieldId);
        }

        BookingDTO bookingDTO = new BookingDTO();
        bookingDTO.setFieldId(fieldId);
        bookingDTO.setBookingTime(selectedDate);

        List<ServiceDTO> services = serviceService.findAllAvailableServices();

        model.addAttribute("bookingDTO", bookingDTO);
        model.addAttribute("locations", locations);
        model.addAttribute("fields", fields);
        model.addAttribute("timeSlots", timeSlots);
        model.addAttribute("selectedField", selectedField);
        model.addAttribute("selectedLocationId", locationId);
        model.addAttribute("selectedFieldId", fieldId);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("services", services);

        return "user/bookings/form";
    }

    @GetMapping("/{id}")
    public String viewBooking(@PathVariable Long id,
                              Model model,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra quyền sở hữu
            if (!bookingService.isBookingOwner(auth.getName(), id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem booking này");
                return "redirect:/user/bookings";
            }

            BookingDTO booking = bookingService.getBookingById(id);
            if (booking == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy booking");
                return "redirect:/user/bookings";
            }

            model.addAttribute("booking", booking);
            return "user/bookings/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/user/bookings";
        }
    }

    @PostMapping("/save")
    public String saveBooking(@ModelAttribute BookingDTO bookingDTO,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            bookingDTO = bookingService.createBooking(bookingDTO, authentication.getName());

            redirectAttributes.addFlashAttribute("successMessage", "Đặt sân thành công!");
            return "redirect:/user/bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/bookings/new?fieldId=" + bookingDTO.getFieldId() +
                    "&date=" + bookingDTO.getBookingTime();
        }
    }


    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            if (!bookingService.isBookingOwner(auth.getName(), id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền hủy đặt sân này");
                return "redirect:/user/bookings";
            }

            bookingService.cancelBooking(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đặt sân thành công");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/user/bookings";
    }
}