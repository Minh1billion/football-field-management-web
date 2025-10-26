package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utescore.dto.LoyaltyDTO;
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

    public LoyaltyDTO getLoyaltyInfo(String username) {
        Loyalty loyalty = loyaltyRepository.findByCustomer_Account_Username(username);

        if (loyalty == null) {
            // Trả về thông tin mặc định nếu chưa có
            LoyaltyDTO dto = new LoyaltyDTO();
            dto.setPoints(0);
            dto.setLevel("BRONZE");
            return dto;
        }

        return convertToDTO(loyalty);
    }

    private LoyaltyDTO convertToDTO(Loyalty loyalty) {
        LoyaltyDTO dto = new LoyaltyDTO();
        dto.setId(loyalty.getId());
        dto.setCustomerId(loyalty.getCustomer().getId());
        dto.setPoints(loyalty.getPoints());
        dto.setLevel(loyalty.getTier().toString());
        return dto;
    }
}