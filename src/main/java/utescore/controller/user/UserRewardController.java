package utescore.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import utescore.dto.LoyaltyDTO;
import utescore.service.RewardService;

@Controller
@RequestMapping("/user/rewards")
@RequiredArgsConstructor
public class UserRewardController {

    private final RewardService rewardService;

    @GetMapping
    public String viewReward(Model model, Authentication authentication) {
        String username = authentication.getName();
        LoyaltyDTO loyalty = rewardService.getLoyaltyInfo(username);

        model.addAttribute("loyalty", loyalty);

        return "user/reward/rewards";
    }
}