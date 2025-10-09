package utescore.service;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utescore.entity.Account;
import utescore.entity.Log;
import utescore.repository.LogRepository;

@Service
public class LogService {
    @Autowired
    private LogRepository logRepository;

    public void logAction(String action, Account account) {
        Log log = new Log();
        log.setAction(action);
        log.setCreatedAt(LocalDateTime.now());
        log.setAccount(account);
        logRepository.save(log);
    }

    public List<Log> getAllLogs() {
        return logRepository.findAll();
    }
}