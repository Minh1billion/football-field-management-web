package utescore.controller.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.dto.RentalDTO;
import utescore.entity.Account;
import utescore.entity.SportWear;
import utescore.service.*;

import java.util.List;

@Controller
@RequestMapping("/user/rentals")
@RequiredArgsConstructor
@SessionAttributes("cartDTO")
public class UserRentalController {

    private final RentalService rentalService;
    private final BookingService bookingService;
    private final SportWearService sportWearService;
    private final VnPayService vnPayService;
    private final AccountService accountService;

    @ModelAttribute("cartDTO")
    public CartDTO cartDTO() {
        return new CartDTO();
    }

    @GetMapping
    public String rentalSportWearsList(Model model, Pageable pageable) {
        model.addAttribute("sportWears", rentalService.getAvailableSportWears(pageable));
        model.addAttribute("myBookings", bookingService.getMyBooking());

        return "user/rentals/list";
    }

    @GetMapping("/detail/{id}")
    public String rentalSportWearDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return rentalService.showSportWearDetail(id, model, redirectAttributes);
    }

    @PostMapping("/add-to-cart/{id}")
    public String addToCart(@PathVariable("id") Long sportWearId,
                            @RequestParam("quantity") int quantity,
                            @RequestParam("rentalDays") int rentalDays,
                            @ModelAttribute("cartDTO") CartDTO cartDTO,
                            RedirectAttributes redirectAttributes) {

        return rentalService.addToCart(cartDTO, sportWearId, quantity, rentalDays, redirectAttributes);
    }

    @GetMapping("/cart")
    public String viewCart(@ModelAttribute("cartDTO") CartDTO cartDTO, Model model) {
        model.addAttribute("cartDTO", cartDTO);
        return "user/rentals/cart";
    }

    @PostMapping("/update-cart")
    public String updateCart(@RequestParam("sportWearId") Long[] sportWearIds,
                             @RequestParam("quantity") int[] quantities,
                             @RequestParam("rentalDays") int[] rentalDays,
                             @ModelAttribute("cartDTO") CartDTO cartDTO) {

        for (int i = 0; i < sportWearIds.length; i++) {
            rentalService.updateCartItem(cartDTO, sportWearIds[i], quantities[i], rentalDays[i]);
        }
        rentalService.recalculateCartTotal(cartDTO);
        return "redirect:/user/rentals/cart";
    }

    @GetMapping("/remove/{id}")  // Thay đổi từ @PostMapping sang @GetMapping
    public String removeFromCart(@PathVariable("id") Long sportWearId,
                                 @ModelAttribute("cartDTO") CartDTO cartDTO,
                                 RedirectAttributes redirectAttributes) {

        boolean removed = cartDTO.getItems().removeIf(
                item -> item.getSportWearId() != null && item.getSportWearId().equals(sportWearId)
        );

        if (removed) {
            rentalService.recalculateCartTotal(cartDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ thuê!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm trong giỏ thuê!");
        }

        return "redirect:/user/rentals/cart";
    }

    @GetMapping("/checkout")
    public String checkout(@ModelAttribute("cartDTO") CartDTO cartDTO,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        // Kiểm tra giỏ hàng rỗng
        if (cartDTO == null || cartDTO.getItems() == null || cartDTO.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
            return "redirect:/user/rentals";
        }

        // Kiểm tra tồn kho cho tất cả items
        for (var item : cartDTO.getItems()) {
            SportWear wear = sportWearService.findById(item.getSportWearId());
            if (wear == null) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Sản phẩm " + item.getName() + " không còn tồn tại!");
                return "redirect:/user/rentals/cart";
            }
            if (item.getQuantity() > wear.getStockQuantity()) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Sản phẩm " + item.getName() + " chỉ còn " + wear.getStockQuantity() + " sản phẩm trong kho!");
                return "redirect:/user/rentals/cart";
            }
        }

        // Lấy thông tin user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountService.findByUsername(username).orElse(null);

        model.addAttribute("cartDTO", cartDTO);
        model.addAttribute("account", account);

        return "user/rentals/checkout";
    }

    @PostMapping("/process-checkout")
    public String processCheckout(@ModelAttribute("cartDTO") CartDTO cartDTO,
                                  @RequestParam("paymentMethod") String paymentMethod,
                                  @RequestParam(value = "customerName", required = false) String customerName,
                                  @RequestParam(value = "customerPhone", required = false) String customerPhone,
                                  @RequestParam(value = "customerAddress", required = false) String customerAddress,
                                  @RequestParam(value = "notes", required = false) String notes,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes,
                                  SessionStatus sessionStatus) {

        try {
            // Kiểm tra giỏ hàng
            if (cartDTO == null || cartDTO.getItems() == null || cartDTO.getItems().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Giỏ hàng của bạn đang trống!");
                return "redirect:/user/rentals";
            }

            // Lưu thông tin khách hàng vào session để dùng sau khi thanh toán
            request.getSession().setAttribute("checkoutCustomerName", customerName);
            request.getSession().setAttribute("checkoutCustomerPhone", customerPhone);
            request.getSession().setAttribute("checkoutCustomerAddress", customerAddress);
            request.getSession().setAttribute("checkoutNotes", notes);

            // Xử lý theo phương thức thanh toán
            if ("VNPAY".equals(paymentMethod)) {
                // Thanh toán qua VNPay
                String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
                String orderInfo = "Thanh toan don hang thue do the thao";
                int totalAmount = cartDTO.getTotalPrice().intValue();

                String vnpayUrl = vnPayService.createOrder(totalAmount, orderInfo, baseUrl);
                return "redirect:" + vnpayUrl;

            } else if ("COD".equals(paymentMethod)) {
                // Thanh toán khi nhận hàng
                String username = SecurityContextHolder.getContext().getAuthentication().getName();

                // Tạo đơn thuê
                Long rentalOrderId = rentalService.createRentalOrder(
                        cartDTO,
                        username,
                        customerName,
                        customerPhone,
                        customerAddress,
                        notes,
                        "COD"
                );

                // Xóa giỏ hàng
                sessionStatus.setComplete();

                redirectAttributes.addFlashAttribute("successMessage",
                        "Đặt hàng thành công! Mã đơn hàng: #" + rentalOrderId);
                return "redirect:/user/rentals/order-success/" + rentalOrderId;

            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Phương thức thanh toán không hợp lệ!");
                return "redirect:/user/rentals/checkout";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/user/rentals/checkout";
        }
    }

    // CALLBACK TỪ VNPAY
    @GetMapping("/payment/callback")
    public String paymentCallback(HttpServletRequest request,
                                  @ModelAttribute("cartDTO") CartDTO cartDTO,
                                  RedirectAttributes redirectAttributes,
                                  SessionStatus sessionStatus) {
        try {
            int paymentStatus = vnPayService.orderReturn(request);

            if (paymentStatus == 1) {
                // Thanh toán thành công
                String username = SecurityContextHolder.getContext().getAuthentication().getName();

                // Lấy thông tin từ session
                String customerName = (String) request.getSession().getAttribute("checkoutCustomerName");
                String customerPhone = (String) request.getSession().getAttribute("checkoutCustomerPhone");
                String customerAddress = (String) request.getSession().getAttribute("checkoutCustomerAddress");
                String notes = (String) request.getSession().getAttribute("checkoutNotes");

                // Tạo đơn thuê
                Long rentalOrderId = rentalService.createRentalOrder(
                        cartDTO,
                        username,
                        customerName,
                        customerPhone,
                        customerAddress,
                        notes,
                        "VNPAY"
                );

                // Xóa thông tin checkout trong session
                request.getSession().removeAttribute("checkoutCustomerName");
                request.getSession().removeAttribute("checkoutCustomerPhone");
                request.getSession().removeAttribute("checkoutCustomerAddress");
                request.getSession().removeAttribute("checkoutNotes");

                // Xóa giỏ hàng
                sessionStatus.setComplete();

                redirectAttributes.addFlashAttribute("successMessage",
                        "Thanh toán thành công! Mã đơn hàng: #" + rentalOrderId);
                return "redirect:/user/rentals/order-success/" + rentalOrderId;

            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thất bại hoặc bị hủy!");
                return "redirect:/user/rentals/checkout";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/user/rentals/checkout";
        }
    }

    @GetMapping("/order-success/{orderId}")
    public String orderSuccess(@PathVariable Long orderId, Model model) {
        // Lấy thông tin đơn hàng để hiển thị
        model.addAttribute("orderId", orderId);
        return "user/rentals/order-success";
    }

    @PostMapping("/add-to-booking")
    public String addToBooking(@RequestParam("sportWearId") Long sportWearId,
                               @RequestParam(value = "serviceId", required = false) Long serviceId,
                               @RequestParam("quantity") int quantity,
                               @RequestParam("bookingId") long bookingId,
                               RedirectAttributes redirectAttributes) {

        try {
            // Validate quantity
            if (quantity <= 0) {
                redirectAttributes.addFlashAttribute("errorMessage", "Số lượng phải lớn hơn 0");
                return "redirect:/user/rentals";
            }

            if (sportWearId != null) {
                // Kiểm tra stock quantity
                SportWear wear = sportWearService.findById(sportWearId);
                if (wear == null) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm");
                    return "redirect:/user/rentals";
                }

                if (quantity > wear.getStockQuantity()) {
                    redirectAttributes.addFlashAttribute("errorMessage",
                            "Số lượng yêu cầu (" + quantity + ") vượt quá tồn kho (" + wear.getStockQuantity() + ")");
                    return "redirect:/user/rentals";
                }

                RentalDTO rental = new RentalDTO();
                rental.setSportWearId(sportWearId);
                rental.setQuantity(quantity);
                rental.setRentalDays(1);
                rentalService.addSportWearToBooking(bookingId, List.of(rental));
            }

            if (serviceId != null) {
                RentalDTO rentalServiceDTO = new RentalDTO();
                rentalServiceDTO.setServiceId(serviceId);
                rentalServiceDTO.setQuantity(1);
                rentalService.addServiceToBooking(bookingId, List.of(rentalServiceDTO));
            }

            redirectAttributes.addFlashAttribute("successMessage", "Thêm vào booking thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Thêm vào booking thất bại: " + e.getMessage());
        }

        return "redirect:/user/bookings/" + bookingId;
    }
}
