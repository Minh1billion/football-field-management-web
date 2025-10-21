package utescore.controller.admin;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.service.AccountService;
import utescore.service.ServiceService;

import java.util.Map;

@Controller
@RequestMapping("/admin")
public class ConfigController {
}
