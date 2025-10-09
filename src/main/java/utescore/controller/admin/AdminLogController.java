package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import utescore.service.LogService;

@Controller
public class AdminLogController {
    @Autowired
    private LogService logService;

    @GetMapping("/admin/logs")
    public String viewLogs(Model model) {
        model.addAttribute("logs", logService.getAllLogs());
        return "admin/log/logs";
    }
}