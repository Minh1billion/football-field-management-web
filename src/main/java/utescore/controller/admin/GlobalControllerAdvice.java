package utescore.controller.admin;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import utescore.util.SecurityUtils;

@ControllerAdvice
public class GlobalControllerAdvice {

    @ModelAttribute
    public void addUsernameToModel(Model model) {
        String username = SecurityUtils.getCurrentUsername();
        model.addAttribute("usernamelogin", username != null ? username : "");
    }
}