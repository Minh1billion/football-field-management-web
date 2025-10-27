package utescore.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    /**
     * ✅ Tự động fix dữ liệu cũ khi khởi động ứng dụng
     */
    @PostConstruct
    public void init() {
        fixOldPosts();
    }

    /**
     * ✅ Lấy tất cả posts, sắp xếp theo ngày tạo (mới nhất trước)
     * Xử lý an toàn với null values
     */
    public List<Post> getAll() {
        return postRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        Post::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }

    /**
     * ✅ Lấy posts theo tác giả
     */
    public List<Post> getByAuthor(String author) {
        return postRepository.findByAuthor(author)
                .stream()
                .sorted(Comparator.comparing(
                        Post::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }

    /**
     * ✅ Lấy một bài viết theo ID
     */
    public Optional<Post> getById(Long id) {
        return postRepository.findById(id);
    }

    /**
     * ✅ Lưu post (tạo mới hoặc cập nhật)
     */
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

    /**
     * ✅ Phê duyệt bài viết
     */
    @Transactional
    public void approve(Long id) {
        postRepository.findById(id).ifPresent(p -> {
            p.setStatus(PostStatus.APPROVED);
            postRepository.save(p);
        });
    }

    /**
     * ✅ Từ chối bài viết
     */
    @Transactional
    public void reject(Long id) {
        postRepository.findById(id).ifPresent(p -> {
            p.setStatus(PostStatus.REJECTED);
            postRepository.save(p);
        });
    }

    /**
     * ✅ Xóa bài viết
     */
    public void delete(Long id) {
        postRepository.deleteById(id);
    }

    /**
     * ✅ Lấy tất cả bài viết đã được duyệt
     */
    public List<Post> getAllApproved() {
        return postRepository.findByStatusOrderByCreatedAtDesc(Post.PostStatus.APPROVED);
    }

    /**
     * ✅ Like/Unlike post - Toggle behavior
     * @param postId ID của post
     * @param username Username của người thích
     * @return LikeResult chứa thông tin kết quả
     */
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

    /**
     * ✅ Helper method để fix dữ liệu cũ
     * Tự động fix các post có createdAt hoặc status = null
     */
    @Transactional
    public void fixOldPosts() {
        try {
            List<Post> allPosts = postRepository.findAll();
            int fixedCount = 0;

            for (Post post : allPosts) {
                boolean needsUpdate = false;

                if (post.getCreatedAt() == null) {
                    post.setCreatedAt(LocalDateTime.now());
                    needsUpdate = true;
                }

                if (post.getStatus() == null) {
                    post.setStatus(PostStatus.PENDING);
                    needsUpdate = true;
                }

                // ✅ Fix likes count dựa trên likedByUsers
                if (post.getLikedByUsers() != null) {
                    int correctLikes = post.getLikedByUsers().size();
                    if (post.getLikes() != correctLikes) {
                        post.setLikes(correctLikes);
                        needsUpdate = true;
                    }
                }

                if (needsUpdate) {
                    postRepository.save(post);
                    fixedCount++;
                }
            }

            if (fixedCount > 0) {
                System.out.println("✅ Fixed " + fixedCount + " old posts");
            } else {
                System.out.println("✅ No posts needed fixing");
            }
        } catch (Exception e) {
            System.err.println("❌ Error fixing old posts: " + e.getMessage());
        }
    }

    /**
     * DTO cho kết quả like/unlike
     */
    public static class LikeResult {
        private final int totalLikes;
        private final boolean isLiked;
        private final boolean success;

        public LikeResult(int totalLikes, boolean isLiked, boolean success) {
            this.totalLikes = totalLikes;
            this.isLiked = isLiked;
            this.success = success;
        }

        public int getTotalLikes() {
            return totalLikes;
        }

        public boolean isLiked() {
            return isLiked;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}