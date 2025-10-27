package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.entity.*;
import utescore.service.ManagerOrderService;

@Controller
@RequestMapping("/manager/orders")
@RequiredArgsConstructor
@Slf4j
public class ManagerOrderController {

    private final ManagerOrderService managerOrderService;

    /**
     * Hiển thị danh sách tất cả đơn hàng (Order, RentalOrder, Booking)
     */
    @GetMapping
    public String viewOrders(@RequestParam(defaultValue = "all") String type,
                             @RequestParam(defaultValue = "all") String status,
                             Model model) {
        log.info("Viewing orders: type={}, status={}", type, status);

        model.addAttribute("type", type);
        model.addAttribute("status", status);
        model.addAttribute("orders", managerOrderService.getAllOrders(type, status));

        return "manager/orders/list";
    }

    /**
     * Chi tiết đơn mua (Order)
     */
    @GetMapping("/sale/{id}")
    public String viewSaleOrderDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Order order = managerOrderService.getSaleOrderById(id);
            model.addAttribute("order", order);
            return "manager/orders/sale-detail";
        } catch (Exception e) {
            log.error("Error viewing sale order: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders";
        }
    }

    /**
     * Chi tiết đơn thuê (RentalOrder)
     */
    @GetMapping("/rental/{id}")
    public String viewRentalOrderDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            RentalOrder order = managerOrderService.getRentalOrderById(id);
            model.addAttribute("order", order);
            return "manager/orders/rental-detail";
        } catch (Exception e) {
            log.error("Error viewing rental order: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders";
        }
    }

    /**
     * Chi tiết booking
     */
    @GetMapping("/booking/{id}")
    public String viewBookingDetail(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Booking booking = managerOrderService.getBookingById(id);
            model.addAttribute("booking", booking);
            return "manager/orders/booking-detail";
        } catch (Exception e) {
            log.error("Error viewing booking: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders";
        }
    }

    /**
     * Cập nhật trạng thái đơn mua (Order)
     */
    @PostMapping("/sale/{id}/update-status")
    public String updateSaleOrderStatus(@PathVariable Long id,
                                        @RequestParam Order.OrderStatus status,
                                        RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.updateSaleOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái đơn hàng thành công!");
            return "redirect:/manager/orders/sale/" + id;
        } catch (Exception e) {
            log.error("Error updating sale order status: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/sale/" + id;
        }
    }

    /**
     * Xác nhận thanh toán COD cho đơn mua
     */
    @PostMapping("/sale/{id}/confirm-payment")
    public String confirmSaleOrderPayment(@PathVariable Long id,
                                          RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.confirmSaleOrderPayment(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận thanh toán COD thành công!");
            return "redirect:/manager/orders/sale/" + id;
        } catch (Exception e) {
            log.error("Error confirming payment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/sale/" + id;
        }
    }

    /**
     * Xác nhận thanh toán COD cho đơn thuê
     */
    @PostMapping("/rental/{id}/confirm-payment")
    public String confirmRentalOrderPayment(@PathVariable Long id,
                                            RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.confirmRentalOrderPayment(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận thanh toán COD thành công!");
            return "redirect:/manager/orders/rental/" + id;
        } catch (Exception e) {
            log.error("Error confirming rental payment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/rental/" + id;
        }
    }

    /**
     * Xác nhận thanh toán CASH/COD cho booking
     */
    @PostMapping("/booking/{id}/confirm-payment")
    public String confirmBookingPayment(@PathVariable Long id,
                                        RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.confirmBookingPayment(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Xác nhận thanh toán thành công!");
            return "redirect:/manager/orders/booking/" + id;
        } catch (Exception e) {
            log.error("Error confirming booking payment: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/booking/" + id;
        }
    }

    /**
     * Xác nhận trả đồ cho đơn thuê (RentalOrder)
     */
    @PostMapping("/rental/{orderId}/return-item/{detailId}")
    public String returnRentalItem(@PathVariable Long orderId,
                                   @PathVariable Long detailId,
                                   @RequestParam(defaultValue = "RETURNED") BookingSportWear.RentalStatus status,
                                   RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.updateRentalItemStatus(detailId, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái trả đồ thành công!");
            return "redirect:/manager/orders/rental/" + orderId;
        } catch (Exception e) {
            log.error("Error returning rental item: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/rental/" + orderId;
        }
    }

    /**
     * Xác nhận trả đồ cho booking
     */
    @PostMapping("/booking/{bookingId}/return-item/{sportWearId}")
    public String returnBookingSportWear(@PathVariable Long bookingId,
                                         @PathVariable Long sportWearId,
                                         @RequestParam(defaultValue = "RETURNED") BookingSportWear.RentalStatus status,
                                         RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.updateBookingSportWearStatus(bookingId, sportWearId, status);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cập nhật trạng thái trả đồ thành công!");
            return "redirect:/manager/orders/booking/" + bookingId;
        } catch (Exception e) {
            log.error("Error returning booking sport wear: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/booking/" + bookingId;
        }
    }

    /**
     * Trả tất cả đồ của đơn thuê
     */
    @PostMapping("/rental/{id}/return-all")
    public String returnAllRentalItems(@PathVariable Long id,
                                       RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.returnAllRentalItems(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xác nhận trả tất cả đồ thuê!");
            return "redirect:/manager/orders/rental/" + id;
        } catch (Exception e) {
            log.error("Error returning all rental items: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/rental/" + id;
        }
    }

    /**
     * Trả tất cả đồ của booking
     */
    @PostMapping("/booking/{id}/return-all")
    public String returnAllBookingSportWears(@PathVariable Long id,
                                             RedirectAttributes redirectAttributes) {
        try {
            managerOrderService.returnAllBookingSportWears(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã xác nhận trả tất cả đồ thuê trong booking!");
            return "redirect:/manager/orders/booking/" + id;
        } catch (Exception e) {
            log.error("Error returning all booking sport wears: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders/booking/" + id;
        }
    }
}