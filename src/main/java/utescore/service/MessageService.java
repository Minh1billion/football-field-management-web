package utescore.service;

import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import utescore.entity.Message;
import utescore.repository.MessageRepository;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository repo;

    public Message save(Message m) {
        return repo.save(m);
    }

    public List<Message> getConversation(String user) {
        return repo.findBySenderOrReceiver(user, user);
    }

    public void recall(Long id) {
        Message m = repo.findById(id).orElseThrow();
        m.setRecalled(true);
        m.setContent("@@@##");
        repo.save(m);
    }

    public List<Message> getHistoryBetween(String user1, String user2) {
        return repo.findBySenderAndReceiver(user1, user2);
    }
}

