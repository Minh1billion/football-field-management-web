package utescore.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utescore.entity.Post;
import utescore.entity.Post.PostStatus;
import utescore.repository.PostRepository;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;

    public List<Post> getAll() {
        return postRepository.findAll().stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .toList();
    }

    public List<Post> getByAuthor(String author) {
        return postRepository.findByAuthor(author)
                .stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .toList();
    }

    public Post save(Post post) {
        return postRepository.save(post);
    }

    public void approve(Long id) {
        postRepository.findById(id).ifPresent(p -> {
            p.setStatus(PostStatus.APPROVED);
            postRepository.save(p);
        });
    }

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

    public Post likePost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        post.setLikes(post.getLikes() + 1);
        return postRepository.save(post);
    }
}
