package utescore.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {
	
	public static String CLOUD_NAME = "dlh6kxhks";
	public static String CLOUD_API_KEY = "894151484976331";
	public static String CLOUD_API_SECRET = "rh5v0BCkHMD0-5gp_sPRXZjI-9Q";
	
	
	public static void setterfull(String a, String b, String c) {
		CLOUD_NAME = a;
		CLOUD_API_KEY = b;
		CLOUD_API_SECRET = c;
	}
	
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

