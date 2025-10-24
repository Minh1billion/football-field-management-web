package utescore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import utescore.entity.Message;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderOrReceiver(String sender, String receiver);
    
    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1) " +
            "ORDER BY m.time ASC")
     List<Message> findBySenderAndReceiver(@Param("user1") String user1, @Param("user2") String user2);
}
