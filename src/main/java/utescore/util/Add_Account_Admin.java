package utescore.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import utescore.dto.RegisterRequest;
import utescore.service.AccountService;

@Component
public class Add_Account_Admin implements CommandLineRunner {

    @Autowired
    private AccountService accountService;

    @Override
    public void run(String... args) throws Exception {
        if (!accountService.existsByUsername("admin")) {
            RegisterRequest adminRequest = new RegisterRequest();
            adminRequest.setUsername("admin");
            adminRequest.setEmail("admin@utescore.com");
            adminRequest.setPassword("admin123");
            adminRequest.setConfirmPassword("admin123");

            accountService.createAccountByRole(adminRequest, "ADMIN");
            System.out.println("Admin account created successfully! tk: admin / admin123");
        } else {
            System.out.println("Admin account already exists! tk: admin / admin123");
        }
    }
}
