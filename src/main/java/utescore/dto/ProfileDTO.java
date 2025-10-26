package utescore.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;
import utescore.entity.Customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

// DTO để hiển thị thông tin profile
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
    private Long accountId;
    private String username;
    private String email;
    private String avatarUrl;

    private Long customerId;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private LocalDate dateOfBirth;
    private Customer.Gender gender;
    private String address;
    private String emergencyContact;
    private String emergencyPhone;

    // Loyalty info
    private Integer loyaltyPoints;
    private String membershipTier;
    private Integer totalBookings;

    // Friend count
    private Integer friendCount;
}
