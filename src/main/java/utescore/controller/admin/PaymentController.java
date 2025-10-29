package utescore.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;
import utescore.dto.FullBillsDTO;
import utescore.service.PaymentService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/payment")
    public String testPayment(@RequestParam(required = false) Integer year, Model model) {
        List<FullBillsDTO> payments = paymentService.getlistBills();

        // Xác định năm (nếu không chọn → năm hiện tại)
        int selectedYear = (year != null) ? year : LocalDate.now().getYear();

        // Lọc theo năm
        List<FullBillsDTO> yearPayments = payments.stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().getYear() == selectedYear)
            .toList();

        List<FullBillsDTO> completedPayments = yearPayments.stream()
            .filter(p -> "COMPLETED".equals(p.getPaymentStatus()))
            .toList();

        // === TỔNG QUAN ===
        long totalBookings = yearPayments.stream().filter(p -> "Booking".equals(p.getType())).count();
        long totalOrders = yearPayments.stream().filter(p -> "Order".equals(p.getType())).count();
        long totalRentals = yearPayments.stream().filter(p -> "Rental".equals(p.getType())).count();

        BigDecimal totalAmount = completedPayments.stream()
            .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // === GROUP BY THÁNG (chỉ 1 lần duyệt) ===
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MM/yyyy");
        Map<String, Long> countByMonth = new LinkedHashMap<>();
        Map<String, BigDecimal> revenueByMonth = new LinkedHashMap<>();

        completedPayments.forEach(p -> {
            String key = p.getCreatedAt().format(monthFmt);
            countByMonth.merge(key, 1L, Long::sum);
            revenueByMonth.merge(key, p.getAmount() == null ? BigDecimal.ZERO : p.getAmount(), BigDecimal::add);
        });

        // Tạo đủ 12 tháng (kể cả tháng 0)
        List<String> monthLabels = new ArrayList<>();
        List<Long> countValues = new ArrayList<>();
        List<BigDecimal> revenueValues = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            String monthKey = String.format("%02d/%d", m, selectedYear);
            monthLabels.add(monthKey);
            countValues.add(countByMonth.getOrDefault(monthKey, 0L));
            revenueValues.add(revenueByMonth.getOrDefault(monthKey, BigDecimal.ZERO));
        }

        // === GỬI DỮ LIỆU ===
        model.addAttribute("payments", yearPayments);
        model.addAttribute("totalBookings", totalBookings);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRentals", totalRentals);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("monthLabels", monthLabels);
        model.addAttribute("countValues", countValues);
        model.addAttribute("revenueValues", revenueValues);
        model.addAttribute("selectedYear", selectedYear);

        // Danh sách năm có dữ liệu
        Set<Integer> availableYears = payments.stream()
            .filter(p -> p.getCreatedAt() != null)
            .map(p -> p.getCreatedAt().getYear())
            .collect(Collectors.toSet());
        model.addAttribute("availableYears", new ArrayList<>(availableYears));

        return "admin/transactions/payment";
    }
    
    @GetMapping("/payment/export-excel")
    public void exportExcel(@RequestParam(required = false) Integer year, HttpServletResponse response) throws IOException {
        int exportYear = (year != null) ? year : LocalDate.now().getYear();
        paymentService.exportExcel(exportYear, response);
    }

}
