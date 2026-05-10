package com.greenlink.greenlink.config;

import com.greenlink.greenlink.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 나중에 Spring Security의 AuthenticationManager를 사용할 수 있도록 등록.
     * 현재 로그인은 AuthService에서 직접 PasswordEncoder로 검증하므로 필수는 아니지만,
     * 확장성을 위해 둔다.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // JWT 방식이므로 세션 사용 안 함
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String path = request.getServletPath();
                            if (path.startsWith("/admin") && !path.contains("/login")) {
                                response.sendRedirect("/admin/login");
                            } else {
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String path = request.getServletPath();
                            if (path.startsWith("/admin") && !path.contains("/login")) {
                                response.sendRedirect("/admin/login");
                            } else {
                                response.sendError(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN, "Forbidden");
                            }
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 허용
                        .requestMatchers("/api/auth/signup", "/api/auth/login").permitAll()

                        // Static resources
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/sb-admin/**", "/vendor/**", "/favicon.ico").permitAll()

                        // Admin Web - Permit both with and without trailing slash
                        .requestMatchers("/admin/login", "/admin/login/").permitAll()
                        .requestMatchers("/admin", "/admin/").hasRole("ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // Admin API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 마스터 조회는 공개 가능
                        .requestMatchers("/api/plants/**").permitAll()
                        .requestMatchers("/api/items/**").permitAll()
                        .requestMatchers("/api/quests/**").permitAll()

                        // IoT 기기용 API
                        .requestMatchers("/api/iot/raspberry/**", "/api/iot/esp/**", "/api/iot/commands/**", "/api/iot/plant-images").permitAll()
                        .requestMatchers("/api/auth/oauth/kakao", "/api/auth/oauth/google").permitAll()
                        
                        .requestMatchers("/error").permitAll()

                        // 그 외 API는 JWT 인증 필요
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}