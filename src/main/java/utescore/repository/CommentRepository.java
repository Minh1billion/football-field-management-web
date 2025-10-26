package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import utescore.entity.Comment;
import utescore.entity.Post;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostOrderByCreatedAtAsc(Post post);
}
