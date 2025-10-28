package utescore.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.Cookie;
import utescore.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authz -> authz
                // Public endpoints (guest xem)
                .requestMatchers("/", "/home","/home/public-home","/maintenance",
                                 "/auth/**", "api/auth/**", "/error").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**").permitAll()
                .requestMatchers("/api/public/**", "/public/**").permitAll()

                // Admin endpoints
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/management/**").hasAnyRole("ADMIN", "MANAGER")

                // Manager endpoints
                .requestMatchers("/manager/**", "/api/manager/**").hasAnyRole("ADMIN", "MANAGER")

                // User endpoints (cần đăng nhập)
                .requestMatchers("/user/**", "/api/user/**", "/profile/**", "/bookings/**", "/orders/**")
                .hasAnyRole("USER", "ADMIN", "MANAGER")

                // Protected API
                .requestMatchers("/api/fields/**", "/api/locations/**", "/api/services/**", "/api/sportwears/**")
                .hasAnyRole("USER", "ADMIN", "MANAGER")

                .anyRequest().authenticated()
            )

            // Cấp authority ROLE_GUEST cho anonymous
            .anonymous(a -> a.authorities("ROLE_GUEST"))

            // Khi chưa đăng nhập mà vào endpoint yêu cầu auth -> redirect về trang login
            .exceptionHandling(e -> e.authenticationEntryPoint(
                (request, response, authException) -> response.sendRedirect("/auth/login?error=access_denied")
            ))

            .authenticationProvider(authenticationProvider())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/home/public-home")
                .addLogoutHandler((request, response, authentication) -> {
                    Cookie cookie = new Cookie("token", null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                })
                .invalidateHttpSession(true)
                .clearAuthentication(true)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}