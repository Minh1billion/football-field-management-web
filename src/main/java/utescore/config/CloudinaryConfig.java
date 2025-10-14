package utescore.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
	
	public static String CLOUD_NAME = ConfigAll.CLOUD_NAME;
	public static String CLOUD_API_KEY = ConfigAll.CLOUD_API_KEY;
	public static String CLOUD_API_SECRET = ConfigAll.CLOUD_API_SECRET;
	
	
	
	
    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", CLOUD_NAME,  
                "api_key", CLOUD_API_KEY,
                "api_secret", CLOUD_API_SECRET,
                "secure", true
        ));
    }
}

