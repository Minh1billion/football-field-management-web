package utescore.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.entity.*;
import utescore.service.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class PaymentController {
    
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final VnPayService vnPayService;
    private final ServiceService serviceService;
    private final SportWearService sportWearService;
    private final OrderItemService orderItemService;
    private final CustomerService customerService;
    private final BooKingService bookingService;
    
    @GetMapping("/testpayment")
    public String testPayment(Model model) {
        List<Order> orders = orderService.getAllOrders();
        List<Payment> payments = paymentService.getAllPayments();
        List<utescore.entity.Service> services = serviceService.findAll();
        List<SportWear> sportWears = sportWearService.findAll();
        List<Customer> customers = customerService.getAllCustomers();
        List<Booking> bookings = bookingService.getAllBookings();
        
        model.addAttribute("orders", orders);
        model.addAttribute("payments", payments);
        model.addAttribute("services", services);
        model.addAttribute("sportWears", sportWears);
        model.addAttribute("customers", customers);
        model.addAttribute("bookings", bookings);
        
        return "admin/thanhtoan/Payment";
    }
    
   
}