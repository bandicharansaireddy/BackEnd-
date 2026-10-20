package com.freshmart.api.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmart.api.exception.ApiExceptionHandler.ApiError;
import org.springframework.web.cors.*;
import java.util.List;
@Configuration
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {
 @Bean UserDetailsService noDefaultDevelopmentUser(){return username->{throw new UsernameNotFoundException("Account not found");};}
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,com.freshmart.api.auth.JwtAuthenticationFilter jwtFilter,ObjectMapper mapper)throws Exception{return http.cors(Customizer.withDefaults()).csrf(csrf->csrf.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(a->a.requestMatchers("/api/products","/api/products/**","/api/categories","/api/categories/**","/api/auth/register","/api/auth/login","/api/auth/refresh","/api/auth/logout","/api/auth/forgot-password","/api/auth/verify-otp","/api/auth/reset-password","/api/dev/otp","/actuator/health").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated()).exceptionHandling(e->e.authenticationEntryPoint((req,res,ex)->{res.setStatus(401);res.setContentType("application/json");mapper.writeValue(res.getOutputStream(),new ApiError(java.time.Instant.now(),401,"Unauthorized","Authentication required",req.getRequestURI()));}).accessDeniedHandler((req,res,ex)->{res.setStatus(403);res.setContentType("application/json");mapper.writeValue(res.getOutputStream(),new ApiError(java.time.Instant.now(),403,"Forbidden","Access denied",req.getRequestURI()));})).httpBasic(b->b.disable()).formLogin(f->f.disable()).addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class).build();}
 @Bean CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String origin){CorsConfiguration c=new CorsConfiguration();c.setAllowedOrigins(List.of(origin));c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));c.setAllowedHeaders(List.of("Authorization","Content-Type","X-XSRF-TOKEN"));c.setAllowCredentials(true);UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource();s.registerCorsConfiguration("/api/**",c);return s;}
}
