package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.service.ReportService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/manager/reports")
@RequiredArgsConstructor
public class ManagerReportController {

    private final ReportService reportService;

    @GetMapping
    public String dashboard(Model model,
                            @RequestParam(required = false) Integer year,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        int y = (year != null) ? year : LocalDate.now().getYear();

        LocalDateTime s = (start != null) ? start.atStartOfDay() : LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime e = (end != null) ? end.atTime(23,59,59) : LocalDate.now().atTime(23,59,59);

        Map<Integer, java.math.BigDecimal> revByMonth = reportService.revenueByMonth(y);
        BigDecimal totalRevenue = reportService.revenueInRange(s, e);
        Map<String, BigDecimal> revenueByField = reportService.revenueByField(s, e);
        Map<utescore.entity.Booking.BookingStatus, Long> bookingStats = reportService.bookingStatusStats();

        model.addAttribute("year", y);
        model.addAttribute("revByMonth", revByMonth);
        model.addAttribute("rangeStart", s);
        model.addAttribute("rangeEnd", e);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("revenueByField", revenueByField);
        model.addAttribute("bookingStats", bookingStats);
        return "manager/reports/dashboard";
    }
}