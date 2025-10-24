package utescore.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.entity.Account;
import utescore.entity.SportWear;
import utescore.service.*;

@Controller
@RequestMapping("/user/sales")
@RequiredArgsConstructor
@SessionAttributes("cartDTO")
@Slf4j
public class UserSaleController {

    private final SaleService saleService;
    private final SportWearService sportWearService;
    private final AccountService accountService;
    private final VnPayService vnPayService;
    private final PaymentService paymentService;

    // Session attribute keys
    private static final String CHECKOUT_CART = "checkoutSaleCartDTO";
    private static final String CHECKOUT_USERNAME = "checkoutSaleUsername";
    private static final String CHECKOUT_NAME = "checkoutSaleCustomerName";
    private static final String CHECKOUT_PHONE = "checkoutSaleCustomerPhone";
    private static final String CHECKOUT_EMAIL = "checkoutSaleCustomerEmail";
    private static final String CHECKOUT_CITY = "checkoutSaleCustomerCity";
    private static final String CHECKOUT_ADDRESS = "checkoutSaleCustomerAddress";
    private static final String CHECKOUT_NOTES = "checkoutSaleNotes";

    @ModelAttribute("cartDTO")
    public CartDTO cartDTO() {
        return new CartDTO();
    }

    @GetMapping
    public String viewSale(Model model, Pageable pageable) {
        log.debug("Viewing sales page: page={}", pageable.getPageNumber());
        model.addAttribute("sportWears", saleService.getAvailableSportWearsForSale(pageable));
        return "user/sales/list";
    }

    @GetMapping("/detail/{id}")
    public String saleDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Viewing sale detail: id={}", id);
        return saleService.showSportWearDetail(id, model, redirectAttributes);
    }

    @PostMapping("/add-to-cart/{id}")
    public String addToCart(@PathVariable("id") Long sportWearId,
                            @RequestParam("quantity") int quantity,
                            @ModelAttribute("cartDTO") CartDTO cartDTO,
                            RedirectAttributes redirectAttributes) {
        log.info("Adding to cart: sportWearId={}, quantity={}", sportWearId, quantity);
        return saleService.addToCart(cartDTO, sportWearId, quantity, redirectAttributes);
    }

    @GetMapping("/cart")
    public String viewCart(@ModelAttribute("cartDTO") CartDTO cartDTO, Model model) {
        log.debug("Viewing cart: {} items", cartDTO.getSaleItems().size());
        model.addAttribute("cartDTO", cartDTO);
        return "user/sales/cart";
    }

    @PostMapping("/update-cart")
    public String updateCart(@RequestParam("sportWearId") Long[] sportWearIds,
                             @RequestParam("quantity") int[] quantities,
                             @ModelAttribute("cartDTO") CartDTO cartDTO) {
        log.info("Updating cart: {} items", sportWearIds.length);

        for (int i = 0; i < sportWearIds.length; i++) {
            saleService.updateCartItem(cartDTO, sportWearIds[i], quantities[i]);
        }
        saleService.recalculateCartTotal(cartDTO);

        return "redirect:/user/sales/cart";
    }

    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable("id") Long sportWearId,
                                 @ModelAttribute("cartDTO") CartDTO cartDTO,
                                 RedirectAttributes redirectAttributes) {
        log.info("Removing from cart: sportWearId={}", sportWearId);

        boolean removed = cartDTO.getSaleItems().removeIf(
                item -> item.getSportWearId() != null && item.getSportWearId().equals(sportWearId)
        );

        if (removed) {
            saleService.recalculateCartTotal(cartDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng!");
            log.info("✅ Item removed from cart");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm trong giỏ hàng!");
            log.warn("⚠️ Item not found in cart");
        }

        return "redirect:/user/sales/cart";
    }

    @GetMapping("/checkout")
    public String checkout(@ModelAttribute("cartDTO") CartDTO cartDTO,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        log.info("Accessing checkout page");

        if (cartDTO == null || cartDTO.getSaleItems() == null || cartDTO.getSaleItems().isEmpty()) {
            log.warn("Empty cart on checkout");
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
            return "redirect:/user/sales";
        }

        // Validate stock
        for (var item : cartDTO.getSaleItems()) {
            SportWear wear = sportWearService.findById(item.getSportWearId());

            if (wear == null) {
                log.warn("Product not found during checkout: id={}", item.getSportWearId());
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Sản phẩm " + item.getName() + " không còn tồn tại!");
                return "redirect:/user/sales/cart";
            }

            if (item.getQuantity() > wear.getStockQuantity()) {
                log.warn("Insufficient stock during checkout: product={}, requested={}, available={}",
                        item.getName(), item.getQuantity(), wear.getStockQuantity());
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Sản phẩm " + item.getName() + " chỉ còn " + wear.getStockQuantity() + " sản phẩm trong kho!");
                return "redirect:/user/sales/cart";
            }
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findByUsername(username).orElse(null);

        model.addAttribute("cartDTO", cartDTO);
        model.addAttribute("account", account);

        log.info("✅ Checkout page loaded: username={}, items={}", username, cartDTO.getSaleItems().size());
        return "user/sales/checkout";
    }

    @PostMapping("/process-checkout")
    public String processCheckout(@ModelAttribute("cartDTO") CartDTO cartDTO,
                                  @RequestParam("paymentMethod") String paymentMethod,
                                  @RequestParam(value = "customerName", required = false) String customerName,
                                  @RequestParam(value = "customerPhone", required = false) String customerPhone,
                                  @RequestParam(value = "customerEmail", required = false) String customerEmail,
                                  @RequestParam(value = "customerCity", required = false) String customerCity,
                                  @RequestParam(value = "customerAddress", required = false) String customerAddress,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes,
                                  SessionStatus sessionStatus) {

        log.info("🔵 Processing checkout: paymentMethod={}", paymentMethod);

        try {
            if (cartDTO == null || cartDTO.getSaleItems() == null || cartDTO.getSaleItems().isEmpty()) {
                log.warn("Empty cart during checkout processing");
                redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
                return "redirect:/user/sales";
            }

            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            log.info("Processing checkout for user: {}", username);

            if ("VNPAY".equals(paymentMethod)) {
                return processVNPayCheckout(cartDTO, username, customerName, customerPhone,
                        customerEmail, customerCity, customerAddress, notes, request);

            } else if ("COD".equals(paymentMethod)) {
                return processCODCheckout(cartDTO, username, customerName, customerPhone,
                        customerEmail, customerCity, customerAddress, notes, redirectAttributes, sessionStatus);

            } else {
                log.warn("Invalid payment method: {}", paymentMethod);
                redirectAttributes.addFlashAttribute("errorMessage", "Phương thức thanh toán không hợp lệ!");
                return "redirect:/user/sales/checkout";
            }

        } catch (Exception e) {
            log.error("❌ Error processing checkout: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/user/sales/checkout";
        }
    }

    private String processVNPayCheckout(CartDTO cartDTO, String username, String customerName,
                                        String customerPhone, String customerEmail, String customerCity,
                                        String customerAddress, String notes, HttpServletRequest request) {
        log.info("Processing VNPay checkout");

        // Lưu thông tin vào session để dùng sau callback
        HttpSession session = request.getSession();
        session.setAttribute(CHECKOUT_CART, cartDTO);
        session.setAttribute(CHECKOUT_USERNAME, username);
        session.setAttribute(CHECKOUT_NAME, customerName);
        session.setAttribute(CHECKOUT_PHONE, customerPhone);
        session.setAttribute(CHECKOUT_EMAIL, customerEmail);
        session.setAttribute(CHECKOUT_CITY, customerCity);
        session.setAttribute(CHECKOUT_ADDRESS, customerAddress);
        session.setAttribute(CHECKOUT_NOTES, notes);

        log.debug("✅ Checkout info saved to session");

        String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
        String orderInfo = "Thanh toan don hang mua do the thao";
        int totalAmount = cartDTO.getTotalPrice().intValue();

        // Tính phí ship
        if (cartDTO.getTotalPrice().doubleValue() < 500000) {
            totalAmount += 30000;
            log.debug("Shipping fee added: 30,000 VND");
        }

        log.info("Creating VNPay payment URL: amount={}", totalAmount);
        String vnpayUrl = vnPayService.createOrderForSales(totalAmount, orderInfo, baseUrl);

        log.info("✅ Redirecting to VNPay: {}", vnpayUrl);
        return "redirect:" + vnpayUrl;
    }

    private String processCODCheckout(CartDTO cartDTO, String username, String customerName,
                                      String customerPhone, String customerEmail, String customerCity,
                                      String customerAddress, String notes, RedirectAttributes redirectAttributes,
                                      SessionStatus sessionStatus) {
        log.info("Processing COD checkout");

        // Tạo đơn hàng mua
        Long saleOrderId = saleService.createSaleOrder(
                cartDTO, username, customerName, customerPhone,
                customerEmail, customerCity, customerAddress, notes, "COD"
        );

        log.info("✅ COD order created: id={}", saleOrderId);

        // Clear session sau khi tạo order thành công
        sessionStatus.setComplete();
        log.debug("Session cleared");

        redirectAttributes.addFlashAttribute("successMessage",
                "Đặt hàng thành công! Mã đơn hàng: #" + saleOrderId);

        return "redirect:/user/sales/order-success/" + saleOrderId;
    }

    @GetMapping("/payment/callback")
    public String paymentCallback(HttpServletRequest request,
                                  RedirectAttributes redirectAttributes,
                                  SessionStatus sessionStatus) {
        log.info("🔵 VNPay payment callback received");

        try {
            int paymentStatus = vnPayService.orderReturn(request);
            log.info("VNPay payment status: {}", paymentStatus);

            if (paymentStatus == 1) {
                return handleSuccessfulPayment(request, redirectAttributes, sessionStatus);
            } else {
                return handleFailedPayment(paymentStatus, redirectAttributes);
            }

        } catch (Exception e) {
            log.error("❌ Error processing payment callback: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/user/sales/checkout";
        }
    }

    private String handleSuccessfulPayment(HttpServletRequest request,
                                           RedirectAttributes redirectAttributes,
                                           SessionStatus sessionStatus) {
        log.info("Processing successful VNPay payment");

        // Lấy thông tin từ session
        HttpSession session = request.getSession();
        CartDTO cartDTO = (CartDTO) session.getAttribute(CHECKOUT_CART);
        String username = (String) session.getAttribute(CHECKOUT_USERNAME);
        String customerName = (String) session.getAttribute(CHECKOUT_NAME);
        String customerPhone = (String) session.getAttribute(CHECKOUT_PHONE);
        String customerEmail = (String) session.getAttribute(CHECKOUT_EMAIL);
        String customerCity = (String) session.getAttribute(CHECKOUT_CITY);
        String customerAddress = (String) session.getAttribute(CHECKOUT_ADDRESS);
        String notes = (String) session.getAttribute(CHECKOUT_NOTES);

        if (cartDTO == null || username == null) {
            log.error("Session expired or missing checkout data");
            redirectAttributes.addFlashAttribute("errorMessage", "Phiên làm việc đã hết hạn!");
            return "redirect:/user/sales";
        }

        // Lấy thông tin từ VNPay
        String vnpTxnRef = request.getParameter("vnp_TxnRef");
        String vnpTransactionNo = request.getParameter("vnp_TransactionNo");

        log.info("VNPay transaction: TxnRef={}, TransactionNo={}", vnpTxnRef, vnpTransactionNo);

        // Tạo đơn mua
        Long saleOrderId = saleService.createSaleOrder(
                cartDTO, username, customerName, customerPhone,
                customerEmail, customerCity, customerAddress, notes, "VNPAY"
        );

        log.info("✅ VNPay order created: id={}", saleOrderId);

        // Cập nhật payment với transactionId từ VNPay
        String transactionId = vnpTransactionNo != null ? vnpTransactionNo : vnpTxnRef;
        paymentService.updatePaymentByOrderId(saleOrderId, transactionId);

        log.info("✅ Payment updated with transaction ID: {}", transactionId);

        // Xóa thông tin checkout trong session
        clearCheckoutSession(session);
        sessionStatus.setComplete();

        log.info("✅ Session cleared");

        redirectAttributes.addFlashAttribute("successMessage",
                "Thanh toán thành công! Mã đơn hàng: #" + saleOrderId);

        return "redirect:/user/sales/order-success/" + saleOrderId;
    }

    private String handleFailedPayment(int paymentStatus, RedirectAttributes redirectAttributes) {
        String errorMessage;

        switch (paymentStatus) {
            case -1:
                errorMessage = "Chữ ký không hợp lệ! Vui lòng thử lại.";
                log.error("Invalid VNPay signature");
                break;
            case -2:
                errorMessage = "Thiếu thông tin bảo mật! Vui lòng thử lại.";
                log.error("Missing VNPay secure hash");
                break;
            case 0:
                errorMessage = "Thanh toán thất bại hoặc bị hủy!";
                log.warn("VNPay payment failed or cancelled");
                break;
            default:
                errorMessage = "Có lỗi xảy ra với thanh toán!";
                log.error("Unknown VNPay payment status: {}", paymentStatus);
        }

        redirectAttributes.addFlashAttribute("errorMessage", errorMessage);
        return "redirect:/user/sales/checkout";
    }

    private void clearCheckoutSession(HttpSession session) {
        session.removeAttribute(CHECKOUT_CART);
        session.removeAttribute(CHECKOUT_USERNAME);
        session.removeAttribute(CHECKOUT_NAME);
        session.removeAttribute(CHECKOUT_PHONE);
        session.removeAttribute(CHECKOUT_EMAIL);
        session.removeAttribute(CHECKOUT_CITY);
        session.removeAttribute(CHECKOUT_ADDRESS);
        session.removeAttribute(CHECKOUT_NOTES);
    }

    @GetMapping("/order-success/{orderId}")
    public String orderSuccess(@PathVariable Long orderId, Model model) {
        log.info("Showing order success page: orderId={}", orderId);
        model.addAttribute("orderId", orderId);
        return "user/sales/order-success";
    }
}