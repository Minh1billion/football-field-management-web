	package utescore.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import utescore.entity.Comment;
import utescore.entity.Post;
import utescore.service.CloudinaryService;
import utescore.service.CommentService;
import utescore.service.PostService;
import utescore.util.SecurityUtils;

import java.io.IOException;

@Controller
@RequestMapping("/user/post")
@RequiredArgsConstructor
public class UserPostController {

    @Autowired
    private PostService postService;

    @Autowired
    private CloudinaryService cloudinaryService;
    
    @Autowired
    private final CommentService commentService;

    @GetMapping
    public String showUserPosts(Model model) {
        String username = SecurityUtils.getCurrentUsername();
        model.addAttribute("posts", postService.getByAuthor(username));
        model.addAttribute("newPost", new Post());
        return "user/post/post";
    }

    @PostMapping("/create")
    public String createPost(@Valid @ModelAttribute("newPost") Post post,
    							BindingResult result,
                             @RequestParam("imageFile") MultipartFile imageFile, Model model) throws IOException {
        if (result.hasErrors()) {
        		model.addAttribute("posts", postService.getByAuthor(SecurityUtils.getCurrentUsername()));
			return "user/post/post";
		}
    	
    		post.setAuthor(SecurityUtils.getCurrentUsername());
        
        if (imageFile != null && !imageFile.isEmpty()) {
            String imageName = cloudinaryService.uploadAndGetName(imageFile);
            post.setImageUrl(cloudinaryService.getImageUrl(imageName));
        }

        postService.save(post);
        return "redirect:/user/post";
    }
    
    @GetMapping("/all-posts")
    public String showAllPosts(Model model) {
        model.addAttribute("posts", postService.getAllApproved());
        model.addAttribute("newComment", new Comment());
        return "user/post/all-posts";
    }

    @MessageMapping("/post/{postId}/like")
    @SendTo("/topic/post/{postId}")
    public LikeResponse likePost(@DestinationVariable Long postId) {
        Post post = postService.likePost(postId);
        return new LikeResponse(post.getLikes());
    }

    @MessageMapping("/post/{postId}/comment")
    @SendTo("/topic/post/{postId}")
    public CommentResponse addComment(@DestinationVariable Long postId, CommentRequest commentRequest) {
        Comment newComment = new Comment();
        newComment.setContent(commentRequest.getContent());
        newComment.setAuthor(commentRequest.getAuthor());
        Comment savedComment = commentService.addComment(postId, newComment);
        return new CommentResponse(savedComment.getAuthor(), savedComment.getContent());
    }

    private static class CommentRequest {
        private String content;
        private String author;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
        public String getAuthor() {
			return author;
		}
        public void setAuthor(String author) {
        		this.author = author;
        }
    }

    private static class LikeResponse {
        private final int likes;

        public LikeResponse(int likes) {
            this.likes = likes;
        }

        public int getLikes() {
            return likes;
        }
    }

    private static class CommentResponse {
        private final String author;
        private final String content;

        public CommentResponse(String author, String content) {
            this.author = author;
            this.content = content;
        }

        public String getAuthor() {
            return author;
        }

        public String getContent() {
            return content;
        }
    }
}
