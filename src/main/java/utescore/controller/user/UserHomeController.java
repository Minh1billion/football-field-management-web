package utescore.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import utescore.service.*;
import utescore.util.SecurityUtils;


@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserHomeController {

    private final BookingService bookingService;
    private final RewardService rewardService;
    private final RentalService rentalService;
    private final NotificationService notificationService;
    private final ReviewService reviewService;

    @GetMapping("/user-home")
    public String home(Model model) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            model.addAttribute("username", username);

            long upcomingBookings = bookingService.countUpcomingBookings(username);
            long monthlySpending = bookingService.calculateMonthlySpending(username);
            long rewardPoints = rewardService.getRewardPoints(username);
            long currentRentals = rentalService.countActiveRentals(username);
            long reviewsCount = reviewService.countReviews(username);
            long unreadNotifications = notificationService.countUnread(username);

            // Gửi dữ liệu sang giao diện
            model.addAttribute("upcomingBookings", upcomingBookings);
            model.addAttribute("monthlySpending", monthlySpending);
            model.addAttribute("rewardPoints", rewardPoints);
            model.addAttribute("currentRentals", currentRentals);
            model.addAttribute("reviewsCount", reviewsCount);
            model.addAttribute("unreadNotifications", unreadNotifications);

        } catch (Exception e) {
            // Nếu có lỗi, gán mặc định 0 để không gây crash view
            model.addAttribute("upcomingBookings", 0L);
            model.addAttribute("monthlySpending", 0L);
            model.addAttribute("rewardPoints", 0L);
            model.addAttribute("currentRentals", 0L);
            model.addAttribute("reviewsCount", 0L);
            model.addAttribute("unreadNotifications", 0L);
        }

        return "user/user-home"; // Trang tương ứng: templates/user/user-home.html
    }
}