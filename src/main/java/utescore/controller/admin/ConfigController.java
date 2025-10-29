package utescore.controller.admin;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import utescore.config.InfoConfig;
import utescore.entity.Log;
import utescore.service.LogService;

@Controller
@RequestMapping("/admin/system-config")
public class ConfigController {
    @Autowired
    private LogService logService;
    @Autowired
    private InfoConfig infoConfig;
    
    @PostMapping("/update-info")
    public String updateInfo(@RequestParam("mail") String mail,
                             @RequestParam("phone") String phone,
                             RedirectAttributes redirectAttributes) {
        infoConfig.setMail(mail);
        infoConfig.setPhone(phone);
        redirectAttributes.addFlashAttribute("message", "✅ Cập nhật thông tin thành công!");
        return "redirect:/admin/system-config";
    }
    
    
    @GetMapping
    public String getPage(Model model) {
        List<Log> list = logService.findByType("MAINTENANCE");
        model.addAttribute("list", list);
        model.addAttribute("infoConfig", infoConfig);
        return "admin/config/config";
    }

    @PostMapping("/add")
    public String add(@RequestParam("start") String start,
                    @RequestParam("end") String end,
                    RedirectAttributes redirectAttributes) {
        try {
        		if (LocalDateTime.parse(end).isBefore(LocalDateTime.parse(start))) {
				redirectAttributes.addFlashAttribute("error", "❌ Thời gian kết thúc phải sau thời gian bắt đầu!");
				return "redirect:/admin/system-config";
			}
            LocalDateTime s = LocalDateTime.parse(start);
            LocalDateTime e = LocalDateTime.parse(end);
            logService.createLogWithUser("Thời gian bảo trì", s, e);
            redirectAttributes.addFlashAttribute("message", "✅ Thêm thời gian bảo trì thành công!");
        } catch (DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("error", "❌ Định dạng thời gian không hợp lệ!");
        }
        return "redirect:/admin/system-config";
    }

    @PostMapping("/update")
    public String update(@RequestParam("id") Long id,
                        @RequestParam("start") String start,
                        @RequestParam("end") String end,
                        RedirectAttributes redirectAttributes) {
        try {
            LocalDateTime s = LocalDateTime.parse(start);
            LocalDateTime e = LocalDateTime.parse(end);
            if (e.isBefore(s)) {
                redirectAttributes.addFlashAttribute("error", "❌ Thời gian kết thúc phải sau thời gian bắt đầu!");
                return "redirect:/admin/system-config";
            }
            Log log = logService.getById(id);
            log.setCreatedAt(s);
            log.setEndDateTime(e);
            logService.save(log);
            redirectAttributes.addFlashAttribute("message", "✅ Cập nhật thời gian bảo trì thành công!");
        } catch (DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("error", "❌ Định dạng thời gian không hợp lệ!");
        }
        return "redirect:/admin/system-config";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            logService.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "✅ Xóa bản ghi bảo trì thành công!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "❌ Xóa bản ghi thất bại!");
        }
        return "redirect:/admin/system-config";
    }
}
