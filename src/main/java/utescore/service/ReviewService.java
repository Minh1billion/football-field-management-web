package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utescore.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public long countReviews(String username) {
        return reviewRepository.countByCustomer_Account_Username(username);
    }
}
