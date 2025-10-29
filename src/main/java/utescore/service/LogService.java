package utescore.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import utescore.entity.Account;
import utescore.entity.Log;
import utescore.entity.Notification;
import utescore.repository.AccountRepository;
import utescore.repository.LogRepository;
@Service
public class LogService {
    @Autowired
    private LogRepository logRepository;
    
    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AccountRepository accountRepository;

    public Log save(Log logs){
        return logRepository.save(logs);
    }
    public Log getById(Long id) {
        return logRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi bảo trì có id: " + id));
    }

    public void deleteById(Long id){
        logRepository.deleteById(id);
    }
    public void logAction(String action, Account account,String type) {
        Log log = new Log();
        log.setAction(action);
        log.setCreatedAt(LocalDateTime.now());
        log.setAccount(account);
        log.setType(type);
        logRepository.save(log);
    }

    public void logAction(String action, Account account) {
        logAction(action, account, "SYSTEM");
    }

    public List<Log> getAllLogs() {
        return logRepository.findAll();
    }

    public List<Log> findByType(String type){
        return logRepository.findAllByType(type);
    }

    public Log createLogWithUser(String action, LocalDateTime s,LocalDateTime e){ 
        if (e.isBefore(s)) {
            throw new RuntimeException("Thời gian kết thúc phải sau thời gian bắt đầu");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); 
        if (authentication == null || !authentication.isAuthenticated()) { 
            throw new RuntimeException("Chưa đăng nhập"); 
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        Account account = accountRepository.findByUsername(username) 
            .orElseThrow(() -> new RuntimeException("Không tìm thấy account: " + username)); 
        Log log = new Log();
        log.setAction(action);
        log.setAccount(account);
        log.setType("MAINTENANCE"); 
        log.setCreatedAt(s);
        log.setEndDateTime(e);
        
        String formattedStart = s.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        String formattedEnd = e.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
        String message = String.format("Hệ thống sẽ bảo trì từ %s đến %s.", formattedStart, formattedEnd);
        notificationService.sendToAllUsers("Thông báo bảo trì hệ thống", message, Notification.NotificationType.GENERAL);
        return logRepository.save(log);
    }
}