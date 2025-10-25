package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.ReviewDTO;
import utescore.entity.Booking;
import utescore.entity.Customer;
import utescore.entity.FootballField;
import utescore.entity.Review;
import utescore.repository.BookingRepository;
import utescore.repository.CustomerRepository;
import utescore.repository.FootballFieldRepository;
import utescore.repository.ReviewRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerRepository customerRepository;
    private final BookingRepository bookingRepository;
    private final FootballFieldRepository fieldRepository;

    public long countReviews(String username) {
        return reviewRepository.countByCustomer_Account_Username(username);
    }

    public List<ReviewDTO> getReviewsByUsername(String username) {
        List<Review> reviews = reviewRepository.findByCustomer_Account_UsernameOrderByCreatedAtDesc(username);
        return reviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ReviewDTO> getReviewsByFieldId(Long fieldId) {
        List<Review> reviews = reviewRepository.findByField_IdOrderByCreatedAtDesc(fieldId);
        return reviews.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ReviewDTO getReviewById(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));
        return convertToDTO(review);
    }

    @Transactional
    public ReviewDTO createReview(ReviewDTO reviewDTO, String username) {
        Customer customer = customerRepository.findByAccount_Username(username);

        Booking booking = bookingRepository.findById(reviewDTO.getBookingId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        // Kiểm tra booking đã hoàn thành chưa
        if (!"COMPLETED".equals(booking.getStatus().name())) {
            throw new RuntimeException("Chỉ có thể đánh giá booking đã hoàn thành");
        }

        // Kiểm tra đã review chưa
        if (reviewRepository.existsByCustomerAndField(customer, booking.getField())) {
            throw new RuntimeException("Bạn đã đánh giá sân này rồi");
        }

        Review review = new Review();
        review.setRating(reviewDTO.getRating());
        review.setComment(reviewDTO.getComment());
        review.setCustomer(customer);
        review.setField(booking.getField());

        review = reviewRepository.save(review);
        return convertToDTO(review);
    }

    public boolean hasReviewedBooking(String username, Long bookingId) {
        Customer customer = customerRepository.findByAccount_Username(username);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        return reviewRepository.existsByCustomerAndField(customer, booking.getField());
    }

    public boolean isReviewOwner(String username, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đánh giá"));

        return review.getCustomer().getAccount().getUsername().equals(username);
    }

    @Transactional
    public void deleteReview(Long reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new RuntimeException("Không tìm thấy đánh giá");
        }
        reviewRepository.deleteById(reviewId);
    }

    private ReviewDTO convertToDTO(Review review) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCustomerName(review.getCustomer().getFullName());
        dto.setFieldName(review.getField().getName());
        dto.setFieldId(review.getField().getId());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }
}