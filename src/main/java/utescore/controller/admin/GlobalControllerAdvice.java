package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import utescore.repository.AccountRepository;
import utescore.util.SecurityUtils;

@ControllerAdvice
public class GlobalControllerAdvice {
	@Autowired
	private AccountRepository accountRepository;
    @ModelAttribute
    public void addUsernameToModel(Model model) {
        String username = SecurityUtils.getCurrentUsername();
        String role = accountRepository.findRoleByUsername(username);
        model.addAttribute("usernamelogin", username != null ? username : "");
        model.addAttribute("rolelogin", role != null ? role : "");
    }
}