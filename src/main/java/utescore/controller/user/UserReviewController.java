package utescore.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.ReviewDTO;
import utescore.service.BookingService;
import utescore.service.FieldManagementService;
import utescore.service.ReviewService;

import java.util.List;

@Controller
@RequestMapping("/user/reviews")
@RequiredArgsConstructor
public class UserReviewController {

    private final ReviewService reviewService;
    private final BookingService bookingService;
    private final FieldManagementService fieldService;

    @GetMapping
    public String viewReviews(Model model, Authentication auth) {
        String username = auth.getName();
        List<ReviewDTO> reviews = reviewService.getReviewsByUsername(username);
        long reviewCount = reviewService.countReviews(username);

        // Tính điểm trung bình
        double averageRating = 0.0;
        if (!reviews.isEmpty()) {
            averageRating = reviews.stream()
                    .mapToInt(ReviewDTO::getRating)
                    .average()
                    .orElse(0.0);
        }

        model.addAttribute("reviews", reviews);
        model.addAttribute("reviewCount", reviewCount);
        model.addAttribute("averageRating", averageRating);
        return "user/reviews/list";
    }

    @GetMapping("/{id}")
    public String viewReviewDetail(@PathVariable Long id, Model model) {
        try {
            ReviewDTO review = reviewService.getReviewById(id);
            model.addAttribute("review", review);
            return "user/reviews/detail";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Không tìm thấy đánh giá");
            return "redirect:/user/reviews";
        }
    }

    @GetMapping("/field/{fieldId}")
    public String viewFieldReviews(@PathVariable Long fieldId, Model model) {
        try {
            List<ReviewDTO> reviews = reviewService.getReviewsByFieldId(fieldId);
            var fieldDTO = fieldService.getFieldById(fieldId);

            double averageRating = 0.0;
            if (!reviews.isEmpty()) {
                averageRating = reviews.stream()
                        .mapToInt(ReviewDTO::getRating)
                        .average()
                        .orElse(0.0);
            }

            model.addAttribute("reviews", reviews);
            model.addAttribute("field", fieldDTO);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("reviewCount", reviews.size());
            return "user/reviews/field-reviews";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/user/fields";
        }
    }

    @GetMapping("/new")
    public String showReviewForm(@RequestParam Long bookingId,
                                 Model model,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();

            // Kiểm tra quyền sở hữu booking
            if (!bookingService.isBookingOwner(username, bookingId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền đánh giá booking này");
                return "redirect:/user/bookings";
            }

            // Kiểm tra xem đã đánh giá chưa
            if (reviewService.hasReviewedBooking(username, bookingId)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn đã đánh giá booking này rồi");
                return "redirect:/user/bookings/" + bookingId;
            }

            ReviewDTO reviewDTO = new ReviewDTO();
            reviewDTO.setBookingId(bookingId);

            model.addAttribute("reviewDTO", reviewDTO);
            model.addAttribute("bookingId", bookingId);

            return "user/reviews/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/user/bookings";
        }
    }

    @PostMapping("/save")
    public String saveReview(@ModelAttribute ReviewDTO reviewDTO,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();
            reviewService.createReview(reviewDTO, username);

            redirectAttributes.addFlashAttribute("successMessage", "Đánh giá của bạn đã được gửi thành công!");
            return "redirect:/user/reviews";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/user/reviews/new?bookingId=" + reviewDTO.getBookingId();
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteReview(@PathVariable Long id,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = auth.getName();

            if (!reviewService.isReviewOwner(username, id)) {
                redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa đánh giá này");
                return "redirect:/user/reviews";
            }

            reviewService.deleteReview(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đánh giá thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/user/reviews";
    }
}