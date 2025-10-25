package utescore.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.PaymentDTO;
import utescore.entity.Account;
import utescore.entity.Customer;
import utescore.entity.Payment;
import utescore.service.AccountService;
import utescore.service.CustomerService;
import utescore.service.PaymentService;
import utescore.service.VnPayService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/user/orders")
@RequiredArgsConstructor
public class UserOrderController {
    private final PaymentService paymentService;
    private final VnPayService vnPayService;
    private final CustomerService customerService; // Cần thêm service này
    private final AccountService accountService; // Hoặc service này

    /**
     * Lấy Customer từ Authentication
     */
    private Customer getCurrentCustomer() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String username = auth.getName();

        // Option 1: Nếu có CustomerService với method findByAccountUsername
        return customerService.findByAccountUsername(username);

        // Option 2: Hoặc qua AccountService
        // Account account = accountService.findByUsername(username);
        // return account.getCustomer();
    }

    @GetMapping
    public String viewOrderList(Model model) {
        try {
            Customer customer = getCurrentCustomer();
            System.out.println("========== VIEW ORDER LIST ==========");
            System.out.println("Customer ID: " + customer.getId());
            System.out.println("Customer Name: " + customer.getFullName());

            // Chỉ lấy payments của customer này
            List<PaymentDTO> orders = paymentService.getPaymentsByCustomerId(customer.getId());
            System.out.println("Number of payments found: " + orders.size());

            if (!orders.isEmpty()) {
                orders.forEach(p -> {
                    System.out.println("Payment: " + p.getPaymentCode() +
                            " - Status: " + p.getStatus() +
                            " - Amount: " + p.getAmount());
                });
            }

            model.addAttribute("orders", orders);
            model.addAttribute("customerName", customer.getFullName());

            return "user/orders/list";
        } catch (Exception e) {
            System.out.println("========== ERROR IN VIEW ORDER LIST ==========");
            e.printStackTrace();
            model.addAttribute("errorMessage", "Không thể tải danh sách đơn hàng: " + e.getMessage());
            model.addAttribute("orders", List.of()); // Thêm empty list để tránh null
            return "user/orders/list";
        }
    }

    @GetMapping("/payment/{id}")
    public String processPayment(@PathVariable Long id,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        try {
            System.out.println("========== BẮT ĐẦU THANH TOÁN ==========");
            System.out.println("Payment ID: " + id);

            // Lấy thông tin customer hiện tại
            Customer customer = getCurrentCustomer();
            System.out.println("Customer ID: " + customer.getId());
            System.out.println("Customer Name: " + customer.getFullName());

            // Lấy payment VÀ verify ownership
            PaymentDTO payment = paymentService.getPaymentByIdAndCustomerId(id, customer.getId());

            if (payment == null) {
                System.out.println("❌ Không tìm thấy payment hoặc không có quyền truy cập");
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Không tìm thấy thông tin thanh toán hoặc bạn không có quyền truy cập!");
                return "redirect:/user/orders";
            }

            System.out.println("Payment Code: " + payment.getPaymentCode());
            System.out.println("Payment Method: " + payment.getPaymentMethod());
            System.out.println("Payment Status: " + payment.getStatus());
            System.out.println("Payment Amount: " + payment.getAmount());

            // Kiểm tra điều kiện thanh toán
            if (!"VNPAY".equals(payment.getPaymentMethod())) {
                System.out.println("❌ Phương thức không phải VNPAY");
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Phương thức thanh toán không hợp lệ!");
                return "redirect:/user/orders";
            }

            if (!"PENDING".equals(payment.getStatus())) {
                System.out.println("❌ Status không phải PENDING");
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Đơn hàng đã được thanh toán hoặc không thể thanh toán!");
                return "redirect:/user/orders";
            }

            // Lưu payment ID và customer ID vào session
            HttpSession session = request.getSession();
            session.setAttribute("paymentId", id);
            session.setAttribute("paymentCode", payment.getPaymentCode());
            session.setAttribute("customerId", customer.getId()); // Thêm customer ID

            System.out.println("✅ Đã lưu vào session:");
            System.out.println("   - paymentId: " + id);
            System.out.println("   - paymentCode: " + payment.getPaymentCode());
            System.out.println("   - customerId: " + customer.getId());
            System.out.println("   - Session ID: " + session.getId());

            // Tạo URL thanh toán VNPAY
            String baseUrl = request.getScheme() + "://" + request.getServerName();
            if (request.getServerPort() != 80 && request.getServerPort() != 443) {
                baseUrl += ":" + request.getServerPort();
            }

            System.out.println("Base URL: " + baseUrl);

            String orderInfo = "Thanh toan don hang " + payment.getPaymentCode();
            String vnpayUrl = vnPayService.createOrder(
                    payment.getAmount().intValue(),
                    orderInfo,
                    baseUrl,
                    "/user/orders/payment/callback"
            );

            System.out.println("VNPAY URL: " + vnpayUrl);
            System.out.println("========== REDIRECT ĐẾN VNPAY ==========");

            return "redirect:" + vnpayUrl;

        } catch (RuntimeException e) {
            System.out.println("========== LỖI AUTHENTICATION ==========");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Vui lòng đăng nhập để thực hiện thanh toán!");
            return "redirect:/login";
        } catch (Exception e) {
            System.out.println("========== LỖI KHI TẠO THANH TOÁN ==========");
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/user/orders";
        }
    }

    @GetMapping("/payment/callback")
    public String paymentCallback(HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        try {
            System.out.println("========== VNPAY CALLBACK START ==========");

            // Validate callback từ VNPAY
            int paymentStatus = vnPayService.orderReturn(request);
            System.out.println("Payment Status từ VNPAY: " + paymentStatus);

            // Lấy thông tin từ session
            HttpSession session = request.getSession();
            Long paymentId = (Long) session.getAttribute("paymentId");
            String paymentCode = (String) session.getAttribute("paymentCode");
            Long customerId = (Long) session.getAttribute("customerId");

            System.out.println("Payment ID từ session: " + paymentId);
            System.out.println("Payment Code từ session: " + paymentCode);
            System.out.println("Customer ID từ session: " + customerId);

            // Verify ownership một lần nữa
            if (paymentId != null && customerId != null) {
                PaymentDTO payment = paymentService.getPaymentByIdAndCustomerId(paymentId, customerId);
                if (payment == null) {
                    System.out.println("❌ Không có quyền truy cập payment này");
                    model.addAttribute("paymentSuccess", false);
                    model.addAttribute("errorMessage", "Không có quyền truy cập giao dịch này");
                    return "user/orders/payment-result";
                }
            }

            // Lấy thông tin từ VNPAY response
            String vnpTxnRef = request.getParameter("vnp_TxnRef");
            String vnpAmount = request.getParameter("vnp_Amount");
            String vnpTransactionNo = request.getParameter("vnp_TransactionNo");
            String vnpResponseCode = request.getParameter("vnp_ResponseCode");
            String vnpOrderInfo = request.getParameter("vnp_OrderInfo");

            System.out.println("vnp_TxnRef: " + vnpTxnRef);
            System.out.println("vnp_TransactionNo: " + vnpTransactionNo);
            System.out.println("vnp_ResponseCode: " + vnpResponseCode);
            System.out.println("vnp_OrderInfo: " + vnpOrderInfo);
            System.out.println("vnp_Amount: " + vnpAmount);

            model.addAttribute("orderId", paymentCode != null ? paymentCode : vnpTxnRef);
            model.addAttribute("transactionNo", vnpTransactionNo);

            if (vnpAmount != null) {
                try {
                    model.addAttribute("amount", Integer.parseInt(vnpAmount));
                } catch (NumberFormatException e) {
                    model.addAttribute("amount", 0);
                }
            }

            if (paymentStatus == 1 && "00".equals(vnpResponseCode)) {
                System.out.println("✅ Thanh toán THÀNH CÔNG - Bắt đầu cập nhật database");

                if (paymentId == null) {
                    System.out.println("⚠️ PaymentId NULL - Sử dụng fallback với paymentCode");

                    if (paymentCode != null) {
                        System.out.println("Cập nhật payment với code: " + paymentCode);
                        Payment updatedPayment = paymentService.updatePaymentStatusByCode(
                                paymentCode,
                                Payment.PaymentStatus.COMPLETED,
                                vnpTransactionNo
                        );
                        System.out.println("Payment sau khi update: " + updatedPayment);
                    } else {
                        System.out.println("❌ KHÔNG có paymentCode trong session!");
                        throw new RuntimeException("Không tìm thấy thông tin thanh toán trong session");
                    }
                } else {
                    System.out.println("✅ Có PaymentId trong session: " + paymentId);

                    Payment updatedPayment = paymentService.updatePaymentStatus(
                            paymentId,
                            Payment.PaymentStatus.COMPLETED
                    );
                    System.out.println("Payment đã cập nhật status COMPLETED");

                    if (vnpTransactionNo != null) {
                        Payment payment = paymentService.findByPaymentCode(paymentCode);
                        if (payment != null) {
                            payment.setTransactionId(vnpTransactionNo);
                            System.out.println("Đã set transaction ID: " + vnpTransactionNo);
                        }
                    }

                    System.out.println("Final Payment Status: " + updatedPayment.getStatus());
                }

                // Xóa thông tin session
                session.removeAttribute("paymentId");
                session.removeAttribute("paymentCode");
                session.removeAttribute("customerId");

                model.addAttribute("paymentSuccess", true);
                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán thành công! Mã giao dịch: " + vnpTransactionNo);

                System.out.println("========== VNPAY CALLBACK SUCCESS ==========");

            } else {
                System.out.println("❌ Thanh toán THẤT BẠI");

                model.addAttribute("paymentSuccess", false);
                model.addAttribute("errorCode", paymentStatus);

                // Xóa thông tin session
                session.removeAttribute("paymentId");
                session.removeAttribute("paymentCode");
                session.removeAttribute("customerId");

                String errorMsg = getVnpayErrorMessage(vnpResponseCode);
                model.addAttribute("errorMessage", errorMsg);

                System.out.println("========== VNPAY CALLBACK FAILED ==========");
            }

            return "user/orders/payment-result";

        } catch (Exception e) {
            System.out.println("========== VNPAY CALLBACK ERROR ==========");
            e.printStackTrace();
            model.addAttribute("paymentSuccess", false);
            model.addAttribute("errorCode", -3);
            model.addAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
            return "user/orders/payment-result";
        }
    }

    private String getVnpayErrorMessage(String responseCode) {
        if (responseCode == null) return "Giao dịch thất bại";

        return switch (responseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking tại ngân hàng";
            case "10" -> "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Đã hết hạn chờ thanh toán";
            case "12" -> "Thẻ/Tài khoản bị khóa";
            case "13" -> "Quý khách nhập sai mật khẩu xác thực giao dịch (OTP)";
            case "24" -> "Giao dịch bị hủy";
            case "51" -> "Tài khoản không đủ số dư để thực hiện giao dịch";
            case "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định";
            default -> "Giao dịch thất bại. Mã lỗi: " + responseCode;
        };
    }
}