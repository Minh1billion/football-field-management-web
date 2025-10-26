package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import utescore.dto.FriendDTO;
import utescore.dto.FriendRequestDTO;
import utescore.dto.ProfileDTO;
import utescore.dto.UpdateProfileDTO;
import utescore.entity.Account;
import utescore.entity.Customer;
import utescore.entity.FriendRequest;
import utescore.entity.Loyalty;
import utescore.repository.AccountRepository;
import utescore.repository.CustomerRepository;
import utescore.repository.FriendRequestRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final CloudinaryService cloudinaryService;
    private final LogService logService;
    private final SimpMessagingTemplate messagingTemplate;

    // ========== PROFILE METHODS ==========

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

    @Transactional
    public ProfileDTO updateProfile(String username, UpdateProfileDTO updateDTO) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Customer customer = account.getCustomer();
        if (customer == null) {
            throw new IllegalArgumentException("Customer profile not found");
        }

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

    @Transactional
    public String updateAvatar(String username, MultipartFile avatarFile) throws IOException {
        if (avatarFile == null || avatarFile.isEmpty()) {
            throw new IllegalArgumentException("Avatar file is required");
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        if (avatarFile.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must not exceed 5MB");
        }

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        String imageName = cloudinaryService.uploadAndGetName(avatarFile);
        String imageUrl = cloudinaryService.getImageUrl(imageName);

        account.setAvatarUrl(imageUrl);
        accountRepository.save(account);

        logService.logAction("Avatar updated", account, "USER");

        return imageUrl;
    }

    @Transactional
    public void removeAvatar(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setAvatarUrl(null);
        accountRepository.save(account);

        logService.logAction("Avatar removed", account, "USER");
    }

    // ========== FRIEND REQUEST METHODS ==========

    @Transactional
    public void sendFriendRequest(String username, String friendUsername) {
        if (username.equals(friendUsername)) {
            throw new IllegalArgumentException("Cannot send friend request to yourself");
        }

        Account sender = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Account receiver = accountRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("Friend account not found"));

        // Kiểm tra đã là bạn chưa
        if (sender.getFriends() != null && sender.getFriends().contains(receiver)) {
            throw new IllegalArgumentException("Already friends with this user");
        }

        // Kiểm tra đã có lời mời pending chưa (cả 2 chiều)
        if (friendRequestRepository.findPendingRequestBetween(
                sender, receiver, FriendRequest.Status.PENDING).isPresent()) {
            throw new IllegalArgumentException("Friend request already exists");
        }

        // Tạo friend request mới
        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequest.Status.PENDING);
        friendRequestRepository.save(request);

        logService.logAction("Sent friend request to: " + friendUsername, sender, "USER");

        // Gửi thông báo real-time qua WebSocket
        messagingTemplate.convertAndSend(
                "/topic/friend-request/" + friendUsername,
                convertToDTO(request)
        );
    }

    @Transactional
    public void acceptFriendRequest(String username, Long requestId) {
        Account receiver = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        // Kiểm tra quyền
        if (!request.getReceiver().equals(receiver)) {
            throw new IllegalArgumentException("Not authorized to accept this request");
        }

        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new IllegalArgumentException("Request is not pending");
        }

        // Cập nhật status
        request.setStatus(FriendRequest.Status.ACCEPTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        // Thêm vào danh sách bạn bè (bidirectional)
        Account sender = request.getSender();

        if (receiver.getFriends() == null) {
            receiver.setFriends(new ArrayList<>());
        }
        if (sender.getFriends() == null) {
            sender.setFriends(new ArrayList<>());
        }

        receiver.getFriends().add(sender);
        sender.getFriends().add(receiver);

        accountRepository.save(receiver);
        accountRepository.save(sender);

        logService.logAction("Accepted friend request from: " + sender.getUsername(), receiver, "USER");

        // Thông báo cho người gửi
        messagingTemplate.convertAndSend(
                "/topic/friend-accepted/" + sender.getUsername(),
                convertToDTO(request)
        );
    }

    @Transactional
    public void rejectFriendRequest(String username, Long requestId) {
        Account receiver = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!request.getReceiver().equals(receiver)) {
            throw new IllegalArgumentException("Not authorized to reject this request");
        }

        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new IllegalArgumentException("Request is not pending");
        }

        request.setStatus(FriendRequest.Status.REJECTED);
        request.setRespondedAt(LocalDateTime.now());
        friendRequestRepository.save(request);

        logService.logAction("Rejected friend request from: " + request.getSender().getUsername(), receiver, "USER");
    }

    @Transactional
    public void cancelFriendRequest(String username, Long requestId) {
        Account sender = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!request.getSender().equals(sender)) {
            throw new IllegalArgumentException("Not authorized to cancel this request");
        }

        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new IllegalArgumentException("Request is not pending");
        }

        friendRequestRepository.delete(request);
        logService.logAction("Cancelled friend request to: " + request.getReceiver().getUsername(), sender, "USER");
    }

    public List<FriendRequestDTO> getReceivedRequests(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return friendRequestRepository
                .findByReceiverAndStatus(account, FriendRequest.Status.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<FriendRequestDTO> getSentRequests(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return friendRequestRepository
                .findBySenderAndStatus(account, FriendRequest.Status.PENDING)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public long getPendingRequestCount(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return friendRequestRepository.countByReceiverAndStatus(account, FriendRequest.Status.PENDING);
    }

    // ========== FRIEND METHODS ==========

    @Transactional
    public void removeFriend(String username, String friendUsername) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Account friend = accountRepository.findByUsername(friendUsername)
                .orElseThrow(() -> new IllegalArgumentException("Friend account not found"));

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
                .map(friend -> convertToFriendDTO(friend, account))
                .collect(Collectors.toList());
    }

    public List<FriendDTO> searchUsers(String username, String searchQuery) {
        Account currentAccount = accountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        List<Account> accounts = accountRepository.findByUsernameContainingIgnoreCase(searchQuery);

        return accounts.stream()
                .filter(account -> !account.getUsername().equals(username))
                .limit(20)
                .map(account -> {
                    FriendDTO dto = convertToFriendDTO(account, currentAccount);

                    // Kiểm tra trạng thái friend request
                    var pendingRequest = friendRequestRepository.findPendingRequestBetween(
                            currentAccount, account, FriendRequest.Status.PENDING
                    );

                    if (pendingRequest.isPresent()) {
                        FriendRequest req = pendingRequest.get();
                        if (req.getSender().equals(currentAccount)) {
                            dto.setRequestStatus("SENT");
                            dto.setRequestId(req.getId());
                        } else {
                            dto.setRequestStatus("RECEIVED");
                            dto.setRequestId(req.getId());
                        }
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

    // ========== HELPER METHODS ==========

    private FriendRequestDTO convertToDTO(FriendRequest request) {
        FriendRequestDTO dto = new FriendRequestDTO();
        dto.setId(request.getId());
        dto.setStatus(request.getStatus().name());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setRespondedAt(request.getRespondedAt());

        Account sender = request.getSender();
        dto.setSenderId(sender.getId());
        dto.setSenderUsername(sender.getUsername());
        dto.setSenderAvatarUrl(sender.getAvatarUrl());
        if (sender.getCustomer() != null) {
            dto.setSenderFullName(sender.getCustomer().getFullName());
        }

        Account receiver = request.getReceiver();
        dto.setReceiverId(receiver.getId());
        dto.setReceiverUsername(receiver.getUsername());
        dto.setReceiverAvatarUrl(receiver.getAvatarUrl());
        if (receiver.getCustomer() != null) {
            dto.setReceiverFullName(receiver.getCustomer().getFullName());
        }

        // Calculate mutual friends
        if (sender.getFriends() != null && receiver.getFriends() != null) {
            int mutualCount = (int) sender.getFriends().stream()
                    .filter(f -> receiver.getFriends().contains(f))
                    .count();
            dto.setMutualFriendsCount(mutualCount);
        }

        return dto;
    }

    private FriendDTO convertToFriendDTO(Account friend, Account currentAccount) {
        FriendDTO dto = new FriendDTO();
        dto.setAccountId(friend.getId());
        dto.setUsername(friend.getUsername());
        dto.setAvatarUrl(friend.getAvatarUrl());

        boolean isFriend = currentAccount.getFriends() != null &&
                currentAccount.getFriends().contains(friend);
        dto.setFriend(isFriend);

        if (friend.getCustomer() != null) {
            dto.setFullName(friend.getCustomer().getFullName());
        }

        // Calculate mutual friends
        if (currentAccount.getFriends() != null && friend.getFriends() != null) {
            int mutualCount = (int) friend.getFriends().stream()
                    .filter(f -> currentAccount.getFriends().contains(f))
                    .count();
            dto.setMutualFriendsCount(mutualCount);
        } else {
            dto.setMutualFriendsCount(0);
        }

        return dto;
    }
}