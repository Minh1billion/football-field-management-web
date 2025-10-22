package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utescore.entity.Loyalty;
import utescore.repository.LoyaltyRepository;

@Service
@RequiredArgsConstructor
public class RewardService {
    private final LoyaltyRepository loyaltyRepository;

    public long getRewardPoints(String username) {
        Loyalty loyalty = loyaltyRepository.findByCustomer_Account_Username(username);
        return loyalty != null ? loyalty.getPoints() : 0;
    }
}
