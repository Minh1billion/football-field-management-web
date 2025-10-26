package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import utescore.dto.FriendDTO;
import utescore.dto.ProfileDTO;
import utescore.dto.UpdateProfileDTO;
import utescore.entity.Account;
import utescore.entity.Customer;
import utescore.entity.Loyalty;
import utescore.repository.AccountRepository;
import utescore.repository.CustomerRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final CloudinaryService cloudinaryService;
    private final LogService logService;

    /**
     * Lấy thông tin profile của user
     */
    public ProfileDTO getProfile(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Customer customer = account.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Customer profile not found");
        }

        ProfileDTO dto = new ProfileDTO();

        // Account info
        dto.setAccountId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setEmail(account.getEmail());
        dto.setAvatarUrl(account.getAvatarUrl());

        // Customer info
        dto.setCustomerId(customer.getId());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setGender(customer.getGender());
        dto.setAddress(customer.getAddress());
        dto.setEmergencyContact(customer.getEmergencyContact());
        dto.setEmergencyPhone(customer.getEmergencyPhone());

        // Loyalty info
        Loyalty loyalty = customer.getLoyalty();
        if (loyalty != null) {
            dto.setLoyaltyPoints(loyalty.getPoints());
            dto.setMembershipTier(loyalty.getTier().name());
            dto.setTotalBookings(loyalty.getTotalBookings());
        }

        // Friend count
        dto.setFriendCount(account.getFriends() != null ? account.getFriends().size() : 0);

        return dto;
    }

    /**
     * Cập nhật thông tin profile
     */
    @Transactional
    public ProfileDTO updateProfile(String username, UpdateProfileDTO updateDTO) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Customer customer = account.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Customer profile not found");
        }

        // Update customer info
        customer.setFirstName(updateDTO.getFirstName());
        customer.setLastName(updateDTO.getLastName());
        customer.setPhoneNumber(updateDTO.getPhoneNumber());
        customer.setDateOfBirth(updateDTO.getDateOfBirth());
        customer.setGender(updateDTO.getGender());
        customer.setAddress(updateDTO.getAddress());
        customer.setEmergencyContact(updateDTO.getEmergencyContact());
        customer.setEmergencyPhone(updateDTO.getEmergencyPhone());

        customerRepository.save(customer);

        logService.logAction("Profile updated", account, "USER");

        return getProfile(username);
    }

    /**
     * Upload và cập nhật avatar
     */
    @Transactional
    public String updateAvatar(String username, MultipartFile avatarFile) throws IOException {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }

        // Validate file type
        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // Validate file size (max 5MB)
        if (avatarFile.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must not exceed 5MB");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Upload to Cloudinary
        String imageName = cloudinaryService.uploadAndGetName(avatarFile);
        String imageUrl = cloudinaryService.getImageUrl(imageName);

        // Update avatar URL
        account.setAvatarUrl(imageUrl);
        accountRepository.save(account);

        logService.logAction("Avatar updated", account, "USER");

        return imageUrl;
    }

    /**
     * Xóa avatar (set về null hoặc default)
     */
    @Transactional
    public void removeAvatar(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setAvatarUrl(null);
        accountRepository.save(account);

        logService.logAction("Avatar removed", account, "USER");
    }

    @Transactional
    public void addFriend(String username, String friendUsername) {
        if (username.equals(friendUsername)) {
            throw new IllegalArgumentException("Cannot add yourself as friend");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Account friend = accountRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("Friend account not found"));

        // Check if already friends
        if (account.getFriends() == null) {
            account.setFriends(new ArrayList<>());
        }

        if (account.getFriends().contains(friend)) {
            throw new IllegalArgumentException("Already friends with this user");
        }

        // Add friend (bidirectional)
        account.getFriends().add(friend);

        if (friend.getFriends() == null) {
            friend.setFriends(new ArrayList<>());
        }
        friend.getFriends().add(account);

        accountRepository.save(account);
        accountRepository.save(friend);

        logService.logAction("Added friend: " + friendUsername, account, "USER");
    }

    @Transactional
    public void removeFriend(String username, String friendUsername) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Account friend = accountRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("Friend account not found"));

        // Remove friend (bidirectional)
        if (account.getFriends() != null) {
            account.getFriends().remove(friend);
        }

        if (friend.getFriends() != null) {
            friend.getFriends().remove(account);
        }

        accountRepository.save(account);
        accountRepository.save(friend);

        logService.logAction("Removed friend: " + friendUsername, account, "USER");
    }

    public List<FriendDTO> getFriends(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getFriends() == null || account.getFriends().isEmpty()) {
            return new ArrayList<>();
        }

        return account.getFriends().stream()
                .map(friend -> {
                    FriendDTO dto = new FriendDTO();
                    dto.setAccountId(friend.getId());
                    dto.setUsername(friend.getUsername());
                    dto.setAvatarUrl(friend.getAvatarUrl());
                    dto.setFriend(true);

                    if (friend.getCustomer() != null) {
                        dto.setFullName(friend.getCustomer().getFullName());
                    }

                    // Calculate mutual friends
                    int mutualCount = 0;
                    if (friend.getFriends() != null) {
                        mutualCount = (int) friend.getFriends().stream()
                                .filter(f -> account.getFriends().contains(f))
                                .count();
                    }
                    dto.setMutualFriendsCount(mutualCount);

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<FriendDTO> searchUsers(String username, String searchQuery) {
        Account currentAccount = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<Account> accounts = accountRepository.findByUsernameContainingIgnoreCase(searchQuery);

        return accounts.stream()
                .filter(account -> !account.getUsername().equals(username)) // Exclude self
                .limit(20) // Limit results
                .map(account -> {
                    FriendDTO dto = new FriendDTO();
                    dto.setAccountId(account.getId());
                    dto.setUsername(account.getUsername());
                    dto.setAvatarUrl(account.getAvatarUrl());

                    if (account.getCustomer() != null) {
                        dto.setFullName(account.getCustomer().getFullName());
                    }

                    // Check if already friend
                    boolean isFriend = currentAccount.getFriends() != null &&
                            currentAccount.getFriends().contains(account);
                    dto.setFriend(isFriend);

                    // Calculate mutual friends
                    if (currentAccount.getFriends() != null && account.getFriends() != null) {
                        int mutualCount = (int) account.getFriends().stream()
                                .filter(f -> currentAccount.getFriends().contains(f))
                                .count();
                        dto.setMutualFriendsCount(mutualCount);
                    } else {
                        dto.setMutualFriendsCount(0);
                    }

                    return dto;
                })
                .collect(Collectors.toList());
    }

    public boolean isFriend(String username, String friendUsername) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Account friend = accountRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("Friend account not found"));

        return account.getFriends() != null && account.getFriends().contains(friend);
    }
}