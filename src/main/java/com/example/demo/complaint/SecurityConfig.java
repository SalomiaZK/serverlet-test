package com.example.demo.complaint;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig implements WebMvcConfigurer {

  /** Single admin account; both env vars are mandatory (startup fails if missing). */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService users(
      @Value("${ADMIN_EMAIL}") String email,
      @Value("${ADMIN_PASSWORD}") String password,
      PasswordEncoder encoder) {
    return new InMemoryUserDetailsManager(
        User.withUsername(email).password(encoder.encode(password)).roles("ADMIN").build());
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    // ponytail: CSRF off (SameSite session cookie only) and in-memory HTTP sessions; on Lambda
    // they die with the container -> switch to Spring Session/JDBC if re-logins get annoying.
    return http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            a ->
                a.requestMatchers("/ping", "/login", "/login.html", "/api/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            f ->
                f.loginPage("/login")
                    .loginProcessingUrl("/api/login")
                    .usernameParameter("email")
                    .successHandler((req, res, auth) -> res.setStatus(200))
                    .failureHandler((req, res, ex) -> res.setStatus(401)))
        .logout(
            l ->
                l.logoutRequestMatcher(new AntPathRequestMatcher("/api/logout", "POST"))
                    .logoutSuccessHandler((req, res, auth) -> res.setStatus(200)))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    new AntPathRequestMatcher("/api/**")))
        .build();
  }

  @Override
  public void addViewControllers(ViewControllerRegistry registry) {
    registry.addViewController("/login").setViewName("forward:/login.html");
  }
}
