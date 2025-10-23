package utescore.util;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import utescore.repository.LogRepository;

@Component
public class MaintenanceInterceptor implements HandlerInterceptor {

	@Autowired
    private LogRepository logRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String uri = request.getRequestURI();

        // Bỏ qua admin, login, public API, trang public, static resources
        if (uri.startsWith("/admin") ||
            uri.startsWith("/auth") ||
            uri.startsWith("/api/auth") ||
            uri.startsWith("/api/public") ||
            uri.startsWith("/home") ||
            uri.startsWith("/maintenance") ||
            uri.startsWith("/css") ||
            uri.startsWith("/js") ||
            uri.startsWith("/images") ||
            uri.startsWith("/webjars") ||
            uri.equals("/favicon.ico")) {
            return true;
        }

        // Kiểm tra maintenance
        boolean underMaintenance = logRepository
                .findActiveMaintenance(LocalDateTime.now())
                .isPresent();
        if (underMaintenance) {
            response.sendRedirect("/maintenance");
            return false;
        }

        return true;
    }
}