package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import utescore.service.PostService;


@Controller
@RequestMapping("/admin/post")
@RequiredArgsConstructor
public class AdminPostController {

    @Autowired
    private PostService postService;

    @GetMapping
    public String listPosts(Model model) {
        model.addAttribute("posts", postService.getAll());
        return "admin/post/post";
    }

    @PostMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {
        postService.approve(id);
        return "redirect:/admin/post";
    }

    @PostMapping("/reject/{id}")
    public String reject(@PathVariable Long id) {
        postService.reject(id);
        return "redirect:/admin/post";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        postService.delete(id);
        return "redirect:/admin/post";
    }
}
