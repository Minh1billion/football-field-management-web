package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.Account;
import utescore.entity.Notification;
import utescore.repository.AccountRepository;
import utescore.repository.NotificationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public List<Notification> listAll() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    public void sendToAccount(Long accountId, String title, String message, Notification.NotificationType type) {
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        Notification n = new Notification();
        n.setAccount(acc);
        n.setTitle(title);
        n.setMessage(message);
        n.setType(type);
        notificationRepository.save(n);
    }

    public int sendToAllUsers(String title, String message, Notification.NotificationType type) {
        List<Account> users = accountRepository.findByRole(Account.Role.USER);
        for (Account acc : users) {
            Notification n = new Notification();
            n.setAccount(acc);
            n.setTitle(title);
            n.setMessage(message);
            n.setType(type);
            notificationRepository.save(n);
        }
        return users.size();
    }

    public long countUnread(String username) {
        return notificationRepository.countUnreadByUsername(username);
    }
}
