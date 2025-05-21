package com.concesionario.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/usuario/Inicio",

                                "/usuario",
                                "/registro",
                                "/vehiculos"
                                ,"/vehiculos/explorar/{id}",
                                "/styles4.css",
                                "/images/**",
                                "/nosotros",
                                "/garantias",
                                "/credito",


                                "/login",
                                "/usuario/agendamiento",
                                "/usuario/loginup",
                                "/css/**",
                                "/js/**",
                                "/STloginup.css",
                                "/uploads/**",
                                "/auth/**"

                        ).permitAll()
                        .requestMatchers("/admin/").hasAnyRole("ADMINISTRADOR", "TRABAJADOR")
                        .requestMatchers("/usuario/cita", "/usuario/cita/guardar").hasAnyRole("USUARIO", "ADMINISTRADOR", "TRABAJADOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler()) // Usamos el successHandler personalizado
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                );
        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            if (authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMINISTRADOR"))) {
                response.sendRedirect("/admin/Dashboard");
            }
            else if (authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_TRABAJADOR"))) {
                response.sendRedirect("/admin/Dashboard");
            }
            else {
                response.sendRedirect("/usuario/Inicio");
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}