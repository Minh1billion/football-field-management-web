package utescore.controller.admin;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import utescore.entity.Poster;
import utescore.service.PosterService;
@Controller
@RequestMapping("/admin/poster")
public class PosterAdminController {

    private final PosterService posterService;

    public PosterAdminController(PosterService posterService) {
        this.posterService = posterService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("posterList", posterService.getAll());
        return "admin/poster/list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("poster", new Poster());
        return "admin/poster/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        Poster poster = posterService.getById(id).orElseThrow();
        model.addAttribute("poster", poster);
        return "admin/poster/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute Poster poster) {
        posterService.save(poster);
        return "redirect:/admin/poster";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Integer id) {
        posterService.delete(id);
        return "redirect:/admin/poster";
    }
}