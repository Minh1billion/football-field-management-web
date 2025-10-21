package utescore.mock;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import utescore.dto.RegisterRequest;
import utescore.service.AccountService;

@Component
public class MockingAccount implements CommandLineRunner {

    @Autowired
    private AccountService accountService;

    @Override
    public void run(String... args) throws Exception {
        String adminUsername = "admin";
        String adminEmail = "admin@utescore.com";
        String adminPassword = "Admin@123"; // bạn có thể đổi sang mật khẩu khác

        // Kiểm tra nếu chưa có admin thì tạo mới
        if (!accountService.existsByUsername(adminUsername)) {
            RegisterRequest request = new RegisterRequest();
            request.setUsername(adminUsername);
            request.setEmail(adminEmail);
            request.setPassword(adminPassword);
            request.setConfirmPassword(adminPassword);
            request.setFirstName("System");
            request.setLastName("Administrator");

            accountService.createAccountByRole(request, "ADMIN");

            System.out.println("✅ Default admin account created:");
            System.out.println("   Username: " + adminUsername);
            System.out.println("   Password: " + adminPassword);
        } else {
            System.out.println("ℹ️ Admin account already exists. Skipping creation.");
        }
    }
}
