package utescore.controller.publicview;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.dto.FootballFieldDTO;
import utescore.dto.ServiceDTO;
import utescore.entity.Post;
import utescore.service.FieldManagementService;
import utescore.service.PostService;
import utescore.service.ServiceService;
import utescore.service.SportWearService;

import java.util.List;

@Controller
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicBrowseController {

    private final FieldManagementService fieldService;
    private final ServiceService serviceService;
    private final SportWearService sportWearService;
    private final PostService postService;

    @GetMapping("/fields")
    public String listFields(Model model) {
        List<FootballFieldDTO> fields = fieldService.listAll();
        model.addAttribute("fields", fields);
        return "public/fields/list";
    }

    @GetMapping("/fields/{id}")
    public String fieldDetail(@PathVariable Long id, Model model) {
        var field = fieldService.getFieldById(id);
        model.addAttribute("field", field);
        return "public/fields/detail";
    }

    @GetMapping("/services")
    public String listServices(Model model) {
        List<ServiceDTO> services = serviceService.findAllAvailableServices();
        model.addAttribute("services", services);
        return "public/services/list";
    }

    @GetMapping("/sportwears")
    public String listSportWears(Model model) {
        model.addAttribute("sportWears", sportWearService.findAll());
        return "public/sportwears/list";
    }

    @GetMapping("/posts")
    public String listApprovedPosts(Model model) {
        List<Post> posts = postService.getAllApproved();
        model.addAttribute("posts", posts);
        return "public/posts/list";
    }
}