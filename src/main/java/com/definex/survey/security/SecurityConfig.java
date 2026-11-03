package com.definex.survey.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * URL seviyesi yetkilendirme. Yollar context-path'e ({@code /api/v1}) göredir.
 * Metot seviyesi kurallar için service/controller'da {@code @PreAuthorize} kullanılır.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Kimlik doğrulama gerektirmeyen uçlar (token alma). */
    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/admin/auth/login",
            "/admin/auth/refresh",
            "/user/auth/login",
            "/user/auth/refresh"
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            ProblemAuthenticationEntryPoint authenticationEntryPoint,
                                            ProblemAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // CSRF koruması kapalı: API stateless ve kimlik bilgisi cookie ile değil Authorization: Bearer
                // başlığıyla taşınıyor. Tarayıcı bu başlığı başka bir siteden gelen isteğe kendiliğinden eklemediği
                // için CSRF saldırısının dayandığı "otomatik gönderilen kimlik bilgisi" durumu oluşmaz.
                // Token ileride cookie'ye taşınırsa CSRF koruması yeniden açılmalıdır.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/admin/**").hasRole(Roles.ADMIN)
                        .requestMatchers("/user/**").hasRole(Roles.USER)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
