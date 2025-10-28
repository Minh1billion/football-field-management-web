package utescore.mock;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import utescore.entity.Post;
import utescore.entity.Comment;
import utescore.repository.PostRepository;
import utescore.repository.CommentRepository;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockingPost implements CommandLineRunner {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public MockingPost(PostRepository postRepository, CommentRepository commentRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Kiểm tra nếu chưa có dữ liệu
        if (postRepository.count() == 0) {
            // Tạo 1 bài viết
            Post post = Post.builder()
                    .author("user")
                    .content("Mai 9h ai đá ở sân A thì để lại số điện thoại")
                    .imageUrl("https://cdnphoto.dantri.com.vn/NOlXkzg6IyjIGmeQIe0of8DpRrA=/thumb_w/1360/2024/08/02/su-pham-ky-thuat-02-crop-1722580675639.jpeg")
                    .status(Post.PostStatus.APPROVED) // Đặt trạng thái là Đã duyệt
                    .build();

            // Tạo 5 comment của user
            List<Comment> comments = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                Comment comment = Comment.builder()
                        .author("user")
                        .content("012932139" + i)
                        .post(post) // liên kết với post
                        .build();
                comments.add(comment);
            }

            // Gắn danh sách comment vào post
            post.setComments(comments);
            post.setLikes(0); 

            // Lưu post sẽ cascade lưu cả comment
            postRepository.save(post);

            System.out.println("Đã thêm 1 bài viết và 5 comment của user!");
        } else {
            System.out.println("Dữ liệu Post đã tồn tại, không thêm mới.");
        }
    }
}
