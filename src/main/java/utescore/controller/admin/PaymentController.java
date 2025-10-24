package utescore.controller.admin;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.dto.BookingDTO;
import utescore.dto.PaymentDTO;
import utescore.entity.*;
import utescore.service.*;
import utescore.service.BookingService;


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
    private final BookingService bookingService;
    
    @GetMapping("/payment")
    public String testPayment(Model model) {
        List<Order> orders = orderService.getAllOrders();
        List<PaymentDTO> payments = paymentService.getAllPayments();
        List<utescore.entity.Service> services = serviceService.findAll();
        List<SportWear> sportWears = sportWearService.findAll();
        List<Customer> customers = customerService.getAllCustomers();
        List<BookingDTO> bookings = bookingService.getAllBookings();
        
        model.addAttribute("orders", orders);
        model.addAttribute("payments", payments);
        model.addAttribute("services", services);
        model.addAttribute("sportWears", sportWears);
        model.addAttribute("customers", customers);
        model.addAttribute("bookings", bookings);
        return "admin/transactions/payment";
    }
}