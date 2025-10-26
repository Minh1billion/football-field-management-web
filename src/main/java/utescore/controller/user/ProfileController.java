package utescore.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.FriendDTO;
import utescore.dto.FriendRequestDTO;
import utescore.dto.ProfileDTO;
import utescore.dto.UpdateProfileDTO;
import utescore.service.ProfileService;
import utescore.util.SecurityUtils;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public String viewProfile(Model model) {
        String username = SecurityUtils.getCurrentUsername();
        ProfileDTO profile = profileService.getProfile(username);

        // Đếm số friend request đang chờ
        long pendingCount = profileService.getPendingRequestCount(username);

        model.addAttribute("profile", profile);
        model.addAttribute("updateDTO", new UpdateProfileDTO());
        model.addAttribute("pendingRequestCount", pendingCount);
        return "user/profile/my-profile";
    }

    @GetMapping("/{username}")
    public String viewUserProfile(@PathVariable String username, Model model, RedirectAttributes redirectAttributes) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        if (currentUsername.equals(username)) {
            return "redirect:/user/profile";
        }

        try {
            ProfileDTO profile = profileService.getProfile(username);
            boolean isFriend = profileService.isFriend(currentUsername, username);

            model.addAttribute("profile", profile);
            model.addAttribute("isFriend", isFriend);
            model.addAttribute("isOwnProfile", false);

            return "user/profile/view-profile";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("message", "User not found");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/user/profile";
        }
    }

    @PostMapping("/update")
    public String updateProfile(@Valid @ModelAttribute UpdateProfileDTO updateDTO,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("message", "Please check your input");
            redirectAttributes.addFlashAttribute("messageType", "danger");
            return "redirect:/user/profile";
        }

        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.updateProfile(username, updateDTO);

            redirectAttributes.addFlashAttribute("message", "Profile updated successfully");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Failed to update profile: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/user/profile";
    }

    @PostMapping("/avatar/upload")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile avatarFile,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.updateAvatar(username, avatarFile);

            redirectAttributes.addFlashAttribute("message", "Avatar uploaded successfully");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("message", "Failed to upload avatar: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/user/profile";
    }

    @PostMapping("/avatar/remove")
    public String removeAvatar(RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.removeAvatar(username);

            redirectAttributes.addFlashAttribute("message", "Avatar removed successfully");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Failed to remove avatar: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        return "redirect:/user/profile";
    }

    // ========== FRIEND MANAGEMENT ==========

    @GetMapping("/friends/manage")
    public String manageFriends(@RequestParam(required = false) String search, Model model) {
        String username = SecurityUtils.getCurrentUsername();

        List<FriendDTO> friends = profileService.getFriends(username);
        model.addAttribute("friends", friends);

        if (search != null && !search.trim().isEmpty()) {
            List<FriendDTO> searchResults = profileService.searchUsers(username, search);
            model.addAttribute("searchResults", searchResults);
            model.addAttribute("searchQuery", search);
        }

        return "user/profile/friends";
    }

    // ========== FRIEND REQUESTS ==========

    @GetMapping("/friends/requests")
    public String friendRequests(Model model) {
        String username = SecurityUtils.getCurrentUsername();

        List<FriendRequestDTO> receivedRequests = profileService.getReceivedRequests(username);
        List<FriendRequestDTO> sentRequests = profileService.getSentRequests(username);

        model.addAttribute("receivedRequests", receivedRequests);
        model.addAttribute("sentRequests", sentRequests);

        return "user/profile/friend-requests";
    }

    // API endpoint for WebSocket badge updates
    @GetMapping("/friends/requests/count")
    @ResponseBody
    public ResponseEntity<Long> getPendingRequestCount() {
        String username = SecurityUtils.getCurrentUsername();
        long count = profileService.getPendingRequestCount(username);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/friends/request/send")
    public String sendFriendRequest(@RequestParam String friendUsername,
                                    @RequestParam(required = false) String returnUrl,
                                    RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.sendFriendRequest(username, friendUsername);

            redirectAttributes.addFlashAttribute("message", "Friend request sent successfully");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/user/profile/friends/manage";
    }

    @PostMapping("/friends/request/accept")
    public String acceptFriendRequest(@RequestParam Long requestId,
                                      @RequestParam(required = false) String returnUrl,
                                      RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.acceptFriendRequest(username, requestId);

            redirectAttributes.addFlashAttribute("message", "Friend request accepted");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/user/profile/friends/requests";
    }

    @PostMapping("/friends/request/reject")
    public String rejectFriendRequest(@RequestParam Long requestId,
                                      @RequestParam(required = false) String returnUrl,
                                      RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.rejectFriendRequest(username, requestId);

            redirectAttributes.addFlashAttribute("message", "Friend request rejected");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/user/profile/friends/requests";
    }

    @PostMapping("/friends/request/cancel")
    public String cancelFriendRequest(@RequestParam Long requestId,
                                      @RequestParam(required = false) String returnUrl,
                                      RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.cancelFriendRequest(username, requestId);

            redirectAttributes.addFlashAttribute("message", "Friend request cancelled");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/user/profile/friends/manage";
    }

    @PostMapping("/friends/add")
    public String addFriend(@RequestParam String friendUsername,
                            @RequestParam(required = false) String returnUrl,
                            RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.sendFriendRequest(username, friendUsername);

            redirectAttributes.addFlashAttribute("message", "Friend request sent successfully");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/user/profile/" + friendUsername;
    }

    @PostMapping("/friends/remove")
    public String removeFriend(@RequestParam String friendUsername,
                               @RequestParam(required = false) String returnUrl,
                               RedirectAttributes redirectAttributes) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            profileService.removeFriend(username, friendUsername);

            redirectAttributes.addFlashAttribute("message", "Friend removed successfully");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }

        if (returnUrl != null && !returnUrl.isEmpty()) {
            return "redirect:" + returnUrl;
        }
        return "redirect:/user/profile/friends/manage";
    }
}