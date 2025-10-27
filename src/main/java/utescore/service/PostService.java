package utescore.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import utescore.entity.Post;
import utescore.entity.Post.PostStatus;
import utescore.repository.PostRepository;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;

    public List<Post> getAll() {
        return postRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Post::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }

    public List<Post> getByAuthor(String author) {
        return postRepository.findByAuthor(author)
                .stream()
                .sorted(Comparator.comparing(
                        Post::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }

    public Optional<Post> getById(Long id) {
        return postRepository.findById(id);
    }

    public Post save(Post post) {
        // ✅ Đảm bảo createdAt và status luôn có giá trị
        if (post.getCreatedAt() == null) {
            post.setCreatedAt(LocalDateTime.now());
        }
        if (post.getStatus() == null) {
            post.setStatus(PostStatus.PENDING);
        }
        return postRepository.save(post);
    }

    @Transactional
    public void approve(Long id) {
        postRepository.findById(id).ifPresent(p -> {
            p.setStatus(PostStatus.APPROVED);
            postRepository.save(p);
        });
    }

    @Transactional
    public void reject(Long id) {
        postRepository.findById(id).ifPresent(p -> {
            p.setStatus(PostStatus.REJECTED);
            postRepository.save(p);
        });
    }

    public void delete(Long id) {
        postRepository.deleteById(id);
    }

    public List<Post> getAllApproved() {
        return postRepository.findByStatusOrderByCreatedAtDesc(Post.PostStatus.APPROVED);
    }

    @Transactional
    public LikeResult toggleLike(Long postId, String username) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        boolean wasLiked = post.isLikedBy(username);
        boolean success;

        if (wasLiked) {
            // Đã thích rồi → Bỏ thích
            success = post.removeLike(username);
        } else {
            // Chưa thích → Thích
            success = post.addLike(username);
        }

        if (success) {
            postRepository.save(post);
        }

        return new LikeResult(post.getLikes(), !wasLiked, success);
    }

    /**
     * @deprecated Dùng toggleLike() thay thế
     */
    @Deprecated
    public Post likePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setLikes(post.getLikes() + 1);
        return postRepository.save(post);
    }

    @Data
    public static class LikeResult {
        private final int totalLikes;
        private final boolean isLiked;
        private final boolean success;

        public LikeResult(int totalLikes, boolean isLiked, boolean success) {
            this.totalLikes = totalLikes;
            this.isLiked = isLiked;
            this.success = success;
        }
    }
}