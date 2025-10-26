package utescore.controller.user;

import java.security.Principal;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;
import utescore.dto.FriendDTO;
import utescore.entity.Account;
import utescore.entity.Message;
import utescore.service.AccountService;
import utescore.service.MessageService;
import utescore.service.ProfileService;
import utescore.util.SecurityUtils;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final AccountService accountService;
    private final ProfileService profileService;

    // ⭐ THÊM MỚI: Route hiển thị trang chat
    @GetMapping("/user/chat")
    public String chatPage(Model model, Principal principal) {
        String currentUsername = principal.getName();

        // Lấy danh sách bạn bè
        List<FriendDTO> friends = profileService.getFriends(currentUsername);

        model.addAttribute("friends", friends);
        model.addAttribute("currentUsername", currentUsername);

        return "user/chat/chat";
    }

    // Gửi tin nhắn đến người nhận cụ thể
    @MessageMapping("/user/chat")
    public void sendMessage(@Payload Message message) {
        Message saved = messageService.save(message);

        // Gửi tin đến người nhận
        messagingTemplate.convertAndSend("/topic/messages/" + message.getReceiver(), saved);

        // Gửi lại cho người gửi (hiển thị chính mình)
        messagingTemplate.convertAndSend("/topic/messages/" + message.getSender(), saved);
    }

    // Thu hồi tin nhắn
    @MessageMapping("/user/recall")
    public void recallMessage(@Payload Map<String, Object> payload) {
        Long messageId = Long.valueOf(payload.get("messageId").toString());
        String sender = payload.get("sender").toString();
        String receiver = payload.get("receiver").toString();

        messageService.recall(messageId);

        // Gửi thông báo thu hồi đến cả hai phía
        messagingTemplate.convertAndSend("/topic/recall/" + sender, messageId);
        messagingTemplate.convertAndSend("/topic/recall/" + receiver, messageId);
    }

    @GetMapping("/user/chat/history/{user}")
    @ResponseBody
    public List<Message> getChatHistory(@PathVariable String user, Principal principal) {
        String currentUser = principal.getName();
        return messageService.getHistoryBetween(currentUser, user);
    }

    @GetMapping("/user/chat/api/accounts")
    @ResponseBody
    public List<Map<String, String>> getAccounts() {
        return accountService.getAllUsers().stream()
                .map(a -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("username", a.getUsername());
                    return map;
                })
                .collect(Collectors.toList());
    }
}