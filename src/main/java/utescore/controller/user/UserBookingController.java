package utescore.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.BookingDTO;
import utescore.dto.FootballFieldDTO;
import utescore.dto.LocationDTO;
import utescore.dto.TimeSlotDTO;
import utescore.service.BookingService;
import utescore.service.FieldManagementService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user/bookings")
@RequiredArgsConstructor
public class UserBookingController {

    private final BookingService bookingService;
    private final FieldManagementService fieldService;

    // ✅ Trang danh sách đặt sân
    @GetMapping
    public String listBookings(Model model, Authentication auth) {
        String username = auth.getName();
        List<BookingDTO> bookings = bookingService.getBookingsByUsername(username);

        // Thống kê
        long upcomingCount = bookingService.countUpcomingBookings(username);
        long monthlySpending = bookingService.calculateMonthlySpending(username);

        model.addAttribute("bookings", bookings);
        model.addAttribute("upcomingCount", upcomingCount);
        model.addAttribute("monthlySpending", monthlySpending);
        return "user/bookings/list";
    }

    // ✅ Trang form tạo đặt sân mới - Bước 1: Chọn sân và ngày
    @GetMapping("/new")
    public String showBookingForm(@RequestParam(required = false) Long locationId,
                                  @RequestParam(required = false) String date,
                                  Model model) {
        LocalDate selectedDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();

        List<LocationDTO> locations = fieldService.getAllLocations();
        List<FootballFieldDTO> fields = fieldService.listAll();

        // Lọc theo location nếu có
        if (locationId != null) {
            fields = fields.stream()
                    .filter(f -> f.getLocationId().equals(locationId))
                    .toList();
        }

        model.addAttribute("bookingDTO", new BookingDTO());
        model.addAttribute("fields", fields);
        model.addAttribute("locations", locations);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedLocationId", locationId);
        return "user/bookings/form";
    }

    // ✅ API: Lấy khung giờ khả dụng (AJAX)
    @GetMapping("/api/time-slots")
    @ResponseBody
    public ResponseEntity<List<TimeSlotDTO>> getTimeSlots(@RequestParam Long fieldId,
                                                          @RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        List<TimeSlotDTO> slots = bookingService.getAvailableTimeSlots(fieldId, localDate);
        return ResponseEntity.ok(slots);
    }

    // ✅ API: Lấy thông tin sân (AJAX)
    @GetMapping("/api/field/{id}")
    @ResponseBody
    public ResponseEntity<FootballFieldDTO> getFieldInfo(@PathVariable Long id) {
        FootballFieldDTO field = fieldService.getFieldById(id);
        return ResponseEntity.ok(field);
    }

    // ✅ Xử lý submit form đặt sân
    @PostMapping("/save")
    public String saveBooking(@ModelAttribute("bookingDTO") BookingDTO bookingDTO,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            BookingDTO savedBooking = bookingService.createBooking(bookingDTO, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt sân thành công! Mã đặt: " + savedBooking.getBookingCode());
            return "redirect:/user/bookings";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/bookings/new?fieldId=" + bookingDTO.getFieldId() +
                    "&date=" + bookingDTO.getBookingTime();
        }
    }

    // ✅ Trang xem sân khả dụng
    @GetMapping("/available")
    public String availableFields(@RequestParam(required = false) Long locationId,
                                  @RequestParam(required = false) String date,
                                  Model model) {
        LocalDate localDate = (date != null) ? LocalDate.parse(date) : LocalDate.now();

        List<FootballFieldDTO> fields = fieldService.findAvailableFields(
                locationId,
                localDate.atTime(8, 0),
                localDate.atTime(22, 0)
        );
        List<LocationDTO> locations = fieldService.getAllLocations();

        model.addAttribute("fields", fields);
        model.addAttribute("locations", locations);
        model.addAttribute("selectedDate", localDate);
        model.addAttribute("selectedLocationId", locationId);
        return "user/bookings/available";
    }

    // ✅ Trang xem chi tiết đặt sân
    @GetMapping("/{id}")
    public String viewBookingDetail(@PathVariable Long id,
                                    Authentication auth,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra quyền sở hữu
            if (!bookingService.isBookingOwner(auth.getName(), id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem đặt sân này");
                return "redirect:/user/bookings";
            }

            BookingDTO booking = bookingService.getBookingById(id);
            model.addAttribute("booking", booking);
            return "user/bookings/detail";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/bookings";
        }
    }

    // ✅ Hủy đặt sân
    @PostMapping("/cancel/{id}")
    public String cancelBooking(@PathVariable Long id,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            // Kiểm tra quyền sở hữu
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

    // ✅ API: Validate thời gian đặt sân
    @PostMapping("/api/validate")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> validateBooking(@RequestBody BookingDTO bookingDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Các validation cơ bản
            if (bookingDTO.getStartTime().isAfter(bookingDTO.getEndTime())) {
                response.put("valid", false);
                response.put("message", "Thời gian kết thúc phải sau thời gian bắt đầu");
                return ResponseEntity.ok(response);
            }

            if (bookingDTO.getStartTime().isBefore(java.time.LocalDateTime.now())) {
                response.put("valid", false);
                response.put("message", "Không thể đặt sân trong quá khứ");
                return ResponseEntity.ok(response);
            }

            response.put("valid", true);
            response.put("message", "Thời gian hợp lệ");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", "Lỗi kiểm tra: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}