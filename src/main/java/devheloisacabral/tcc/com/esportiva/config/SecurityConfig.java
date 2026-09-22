package devheloisacabral.tcc.com.esportiva.config;

import devheloisacabral.tcc.com.esportiva.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/health", "/auth/login", "/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/alunos", "/planos", "/mensalidades").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/alunos/*/plano").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/mensalidades/*/receber", "/mensalidades/*/cancelar").hasRole("ADMIN")
                        .requestMatchers("/professores", "/professores/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/turmas", "/turmas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/turmas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/turmas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/turmas/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/turmas", "/turmas/**").hasAnyRole("ADMIN", "PROFESSOR")
                        .requestMatchers(HttpMethod.POST, "/matriculas").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/matriculas").hasAnyRole("ADMIN", "ALUNO")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
