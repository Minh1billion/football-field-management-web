package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import utescore.config.CloudinaryConfig;
import utescore.service.AccountService;
import utescore.service.ServiceService;

@Controller
@RequestMapping("/admin")
public class ConfigController {
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private ServiceService serviceService;
    
    private String cloudName = CloudinaryConfig.CLOUD_NAME;
    private String cloudApiKey = CloudinaryConfig.CLOUD_API_KEY;
    private String cloudApiSecret = CloudinaryConfig.CLOUD_API_SECRET;
    
    @GetMapping("/system-config")
    public String dashboard(Model model) {
        model.addAttribute("cloudName", cloudName);
        model.addAttribute("cloudApiKey", cloudApiKey);
        model.addAttribute("cloudApiSecret", cloudApiSecret);
        return "admin/Config/system-config";
    }
    
    @PostMapping("/system-config")
    public String updateConfig(@RequestParam("cloudName") String cloudName,
                             @RequestParam("cloudApiKey") String cloudApiKey,
                             @RequestParam("cloudApiSecret") String cloudApiSecret,
                             RedirectAttributes redirectAttributes) {
        try {
            CloudinaryConfig.setterfull(cloudName, cloudApiKey, cloudApiSecret);
            
            redirectAttributes.addFlashAttribute("successMessage", "Cấu hình đã được cập nhật thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật cấu hình: " + e.getMessage());
        }
        
        return "redirect:/admin/system-config";
    }
}