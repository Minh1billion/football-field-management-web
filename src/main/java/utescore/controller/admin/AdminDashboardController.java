package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import utescore.repository.FootballFieldRepository;
import utescore.service.AccountService;
import utescore.service.ServiceService;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private ServiceService serviceService;
    
    @Autowired
    private FootballFieldRepository footballFieldRepository;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        try {
            // Lấy thống kê tài khoản
            long totalAccounts = accountService.countAllAccounts();
            long totalUsers = accountService.countByRole("USER");
            long totalManagers = accountService.countByRole("MANAGER");
            long totalAdmins = accountService.countByRole("ADMIN");
            long activeAccounts = accountService.countActiveAccounts();
            long inactiveAccounts = totalAccounts - activeAccounts;
            
            // Lấy thống kê dịch vụ
            long totalServices = serviceService.countAllServices();
            
            Long countFields = footballFieldRepository.findByIsActiveTrue().stream().count();
            // Thống kê sân (tạm thời để 0, sẽ implement sau)
            long totalFields = countFields;
            
            // Thêm dữ liệu vào model
            model.addAttribute("totalAccounts", totalAccounts);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalManagers", totalManagers);
            model.addAttribute("totalAdmins", totalAdmins);
            model.addAttribute("activeAccounts", activeAccounts);
            model.addAttribute("inactiveAccounts", inactiveAccounts);
            model.addAttribute("totalServices", totalServices);
            model.addAttribute("totalFields", totalFields);
            
        } catch (Exception e) {
            // Nếu có lỗi, set giá trị mặc định
            model.addAttribute("totalAccounts", 0L);
            model.addAttribute("totalUsers", 0L);
            model.addAttribute("totalManagers", 0L);
            model.addAttribute("totalAdmins", 0L);
            model.addAttribute("activeAccounts", 0L);
            model.addAttribute("inactiveAccounts", 0L);
            model.addAttribute("totalServices", 0L);
            model.addAttribute("totalFields", 0L);
        }
        
        return "admin/dashboard";
    }
}