package utescore.controller.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public String createPost(@ModelAttribute Post post,
                             @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
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
        String username = SecurityUtils.getCurrentUsername();
        model.addAttribute("posts", postService.getAllApproved());
        model.addAttribute("newComment", new Comment());
        model.addAttribute("usernamelogin", username);
        return "user/post/all-posts";
    }

    // ✅ NEW: Toggle like với username
    @MessageMapping("/post/{postId}/like")
    @SendTo("/topic/post/{postId}")
    public LikeResponse toggleLike(@DestinationVariable Long postId, LikeRequest likeRequest) {
        PostService.LikeResult result = postService.toggleLike(postId, likeRequest.getUsername());
        return new LikeResponse(result.getTotalLikes(), result.isLiked());
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

    // ✅ NEW: Request cho toggle like
    private static class LikeRequest {
        private String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
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

    // ✅ UPDATED: Response bao gồm cả trạng thái isLiked
    private static class LikeResponse {
        private final int likes;
        private final boolean isLiked;

        public LikeResponse(int likes, boolean isLiked) {
            this.likes = likes;
            this.isLiked = isLiked;
        }

        public int getLikes() {
            return likes;
        }

        public boolean isLiked() {
            return isLiked;
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