package utescore.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import utescore.util.MaintenanceInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private MaintenanceInterceptor maintenanceInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(maintenanceInterceptor)
                .addPathPatterns("/**");
    }
}