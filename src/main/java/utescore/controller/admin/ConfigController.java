package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import utescore.config.ConfigAll;
import utescore.service.AccountService;
import utescore.service.ServiceService;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class ConfigController {
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private ServiceService serviceService;
    
    private String cloudName = ConfigAll.CLOUD_NAME;
    private String cloudApiKey = ConfigAll.CLOUD_API_KEY;
    private String cloudApiSecret = ConfigAll.CLOUD_API_SECRET;
    
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
            // Validate Cloudinary API before saving
            Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", cloudApiKey,
                    "api_secret", cloudApiSecret,
                    "secure", true
            ));
            
            // Test connection by getting usage information
            Map<String, Object> result = cloudinary.api().usage(ObjectUtils.emptyMap());
            System.out.println("Cloudinary validation successful: " + result);
            
            // If validation passes, save the configuration
            ConfigAll.setterfull(cloudName, cloudApiKey, cloudApiSecret);
            System.out.println("Cấu hình mới đã được lưu: " + ConfigAll.CLOUD_NAME + ", " + ConfigAll.CLOUD_API_KEY);
            redirectAttributes.addFlashAttribute("successMessage", "Cấu hình Cloudinary đã được xác thực và lưu thành công!");
            
        } catch (Exception e) {
            System.err.println("Cloudinary validation failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Không thể lưu cấu hình - API Cloudinary không hợp lệ: " + e.getMessage());
        }
        
        return "redirect:/admin/system-config";
    }
}