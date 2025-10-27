package utescore.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.Post;
import utescore.entity.Post.PostStatus;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByAuthor(String author);

	List<Post> findByStatusOrderByCreatedAtDesc(PostStatus approved);
}