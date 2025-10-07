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
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import utescore.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final CustomAuthenticationFailureHandler failureHandler;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          CustomAuthenticationSuccessHandler successHandler,
                          CustomAuthenticationFailureHandler failureHandler) {
        this.userDetailsService = userDetailsService;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authenticationProvider(authenticationProvider())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        // Public endpoints
                        .requestMatchers("/", "/home", "/login", "/register", "/signup", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico", "/webjars/**").permitAll()
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/api/fields/public/**", "/api/locations/public/**").permitAll()
                        .requestMatchers("/api/services/public/**", "/api/sportwears/public/**").permitAll()

                        // Admin endpoints
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/management/**").hasAnyRole("ADMIN", "MANAGER")

                        // Manager endpoints
                        .requestMatchers("/manager/**", "/api/manager/**").hasAnyRole("ADMIN", "MANAGER")

                        // User endpoints
                        .requestMatchers("/user/**", "/api/user/**", "/profile/**", "/bookings/**", "/orders/**")
                        .hasAnyRole("USER", "ADMIN", "MANAGER")

                        // Protected API endpoints
                        .requestMatchers("/api/fields/**", "/api/locations/**", "/api/services/**", "/api/sportwears/**")
                        .hasAnyRole("USER", "ADMIN", "MANAGER")

                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/perform-login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(successHandler)
                        .failureHandler(failureHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // không cần AntPathRequestMatcher nữa
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .clearAuthentication(true)
                        .permitAll()
                )
                .sessionManagement(session ->
                        session
                                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.IF_REQUIRED)
                                .sessionConcurrency(concurrency -> concurrency
                                        .maximumSessions(1)
                                        .maxSessionsPreventsLogin(false)
                                        .sessionRegistry(sessionRegistry())
                                )
                                .sessionFixation(sessionFix -> sessionFix.migrateSession())
                )
                .rememberMe(remember -> remember
                        .key("utescore-remember-me-key")
                        .tokenValiditySeconds(24 * 60 * 60)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                )
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedPage("/access-denied")
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendRedirect("/login?error=access_denied"))
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.disable())
                        .httpStrictTransportSecurity(hsts -> hsts.disable())
                );

        return http.build();
    }
}
