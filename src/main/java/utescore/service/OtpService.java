package utescore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.length:6}")
    private int otpLength;

    private final JavaMailSender mailSender;
    private final ConcurrentHashMap<String, OtpData> otpStorage = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void generateAndSendOtp(String email) {
        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(otpExpiryMinutes);
        
        otpStorage.put(email, new OtpData(otp, expiryTime));
        
        sendOtpEmail(email, otp);
    }

    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpStorage.get(email);
        
        if (otpData == null) {
            return false;
        }
        
        if (LocalDateTime.now().isAfter(otpData.getExpiryTime())) {
            otpStorage.remove(email);
            return false;
        }
        
        boolean isValid = otpData.getOtp().equals(otp);
        if (isValid) {
            otpStorage.remove(email);
        }
        
        return isValid;
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtpEmail(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your OTP Verification Code");
        message.setText("Your OTP verification code is: " + otp + "\n\n" +
                       "This code will expire in " + otpExpiryMinutes + " minutes.\n" +
                       "Please do not share this code with anyone.");
        
        mailSender.send(message);
    }

    private static class OtpData {
        private final String otp;
        private final LocalDateTime expiryTime;

        public OtpData(String otp, LocalDateTime expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        public String getOtp() { return otp; }
        public LocalDateTime getExpiryTime() { return expiryTime; }
    }
}