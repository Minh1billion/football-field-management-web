package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import utescore.service.BookingService;
import utescore.service.FieldManagementService;
import utescore.service.MaintenanceManagementService;
import utescore.util.SecurityUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerDashboardController {

    private final FieldManagementService fieldManagementService;
    private final BookingService bookingService;
    private final MaintenanceManagementService maintenanceService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            String currentUsername = SecurityUtils.getCurrentUsername();

            // 1. Tổng số sân do manager này quản lý
            long totalFields = fieldManagementService.countFieldsByManager(currentUsername);

            // 2. Lịch đặt sân hôm nay
            LocalDate today = LocalDate.now();
            long todayBookings = bookingService.countBookingsByManagerAndDate(currentUsername, today);

            // 3. Bảo trì sắp tới (trong 7 ngày tới)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextWeek = now.plusDays(7);
            long upcomingMaintenances = maintenanceService.countUpcomingMaintenanceByManager(
                    currentUsername, now, nextWeek);

            // 4. Doanh thu tháng này
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
            BigDecimal monthlyRevenue = bookingService.calculateCompletedRevenueByManagerAndDateRange(
            	    currentUsername, startOfMonth, endOfMonth);

            // 5. Khách hàng hoạt động (đặt sân ít nhất 1 lần trong tháng)
            long activeCustomers = bookingService.countActiveCustomersByManagerAndMonth(
                    currentUsername, today.getMonthValue(), today.getYear());

            // 6. Sân đang bảo trì
            long maintenanceFields = maintenanceService.countActiveMaintenanceFieldsByManager(currentUsername);

            // Thêm các thuộc tính vào model
            model.addAttribute("totalFields", totalFields);
            model.addAttribute("todayBookings", todayBookings);
            model.addAttribute("upcomingMaintenances", upcomingMaintenances);
            model.addAttribute("monthlyRevenue", formatCurrency(monthlyRevenue));
            model.addAttribute("activeCustomers", activeCustomers);
            model.addAttribute("maintenanceFields", maintenanceFields);

            // Thống kê bổ sung
            // Số lịch đặt chờ xác nhận
            long pendingBookings = bookingService.countPendingBookingsByManager(currentUsername);
            model.addAttribute("pendingBookings", pendingBookings);

            // Tỷ lệ sử dụng sân hôm nay (%)
            double occupancyRate = calculateOccupancyRate(currentUsername, today);
            model.addAttribute("occupancyRate", String.format("%.1f", occupancyRate));

            // Số sân đang hoạt động
            long activeFields = fieldManagementService.countActiveFieldsByManager(currentUsername);
            model.addAttribute("activeFields", activeFields);

        } catch (Exception e) {
            // Nếu có lỗi, set giá trị mặc định
            model.addAttribute("totalFields", 0L);
            model.addAttribute("todayBookings", 0L);
            model.addAttribute("upcomingMaintenances", 0L);
            model.addAttribute("monthlyRevenue", "0");
            model.addAttribute("activeCustomers", 0L);
            model.addAttribute("maintenanceFields", 0L);
            model.addAttribute("pendingBookings", 0L);
            model.addAttribute("occupancyRate", "0.0");
            model.addAttribute("activeFields", 0L);

            // Log lỗi
            e.printStackTrace();
        }

        return "manager/dashboard";
    }

    /**
     * Tính tỷ lệ sử dụng sân trong ngày
     */
    private double calculateOccupancyRate(String managerUsername, LocalDate date) {
        try {
            long totalFields = fieldManagementService.countActiveFieldsByManager(managerUsername);
            if (totalFields == 0) return 0.0;

            // Giả sử mỗi sân có 14 giờ hoạt động (8:00 - 22:00)
            long totalSlots = totalFields * 14;

            // Đếm số giờ đã được đặt
            long bookedSlots = bookingService.countBookedSlotsByManagerAndDate(managerUsername, date);

            return (bookedSlots * 100.0) / totalSlots;
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Format tiền tệ
     */
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        return String.format("%,.0f", amount);
    }
}