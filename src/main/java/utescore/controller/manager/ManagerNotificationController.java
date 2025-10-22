package utescore.controller.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.entity.Notification;
import utescore.service.NotificationService;

@Controller
@RequestMapping("/manager/notifications")
@RequiredArgsConstructor
public class ManagerNotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("notifications", notificationService.listAll());
        return "manager/notifications/list";
    }

    @GetMapping("/compose")
    public String compose() {
        return "manager/notifications/compose";
    }

    @PostMapping("/send/all")
    public String sendAll(@RequestParam String title,
                          @RequestParam String message,
                          @RequestParam(defaultValue = "GENERAL") Notification.NotificationType type) {
        notificationService.sendToAllUsers(title, message, type);
        return "redirect:/manager/notifications";
    }

    @PostMapping("/send/{accountId}")
    public String sendOne(@PathVariable Long accountId,
                          @RequestParam String title,
                          @RequestParam String message,
                          @RequestParam(defaultValue = "GENERAL") Notification.NotificationType type) {
        notificationService.sendToAccount(accountId, title, message, type);
        return "redirect:/manager/notifications";
    }
}