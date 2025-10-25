package utescore.config;

import org.springframework.stereotype.Component;
import lombok.Data;

@Component
@Data
public class InfoConfig {
    private String mail = "MinhCofig@gmail.com";
    private String phone = "0123456789";
}