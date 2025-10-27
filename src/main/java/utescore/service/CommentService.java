package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utescore.entity.Comment;
import utescore.entity.Post;
import utescore.repository.CommentRepository;
import utescore.repository.PostRepository;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    public Comment addComment(Long postId, Comment comment) {
        Post post = postRepository.findById(postId).orElseThrow();
        comment.setPost(post);
        return commentRepository.save(comment);
    }
}
