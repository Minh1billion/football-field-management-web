package utescore.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dlh6kxhks",   // 👈 cloud_name của bạn
                "api_key", "894151484976331",
                "api_secret", "rh5v0BCkHMD0-5gp_sPRXZjI-9Q",
                "secure", true
        ));
    }
}

