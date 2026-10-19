package com.book.core.auth.infrastructure.security;

import com.book.core.user.application.usecase.CheckUserRestrictionUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(final HttpSecurity http, @Qualifier("serviceJwtDecoder") final JwtDecoder serviceJwtDecoder,
        final JwtAuthenticationEntryPoint authenticationEntryPoint, final CorsConfigurationSource corsConfigurationSource,
        final CheckUserRestrictionUseCase checkUserRestrictionUseCase) throws Exception {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource)).csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(authorize -> authorize.requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/items", "/api/v1/categories", "/api/v1/products/*", "/api/v1/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/coupons").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/*/login", "/api/v1/auth/*/signup", "/api/v1/auth/reissue").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(authenticationEntryPoint))
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(serviceJwtDecoder)
                    .jwtAuthenticationConverter(new RestrictedUserJwtAuthenticationConverter(checkUserRestrictionUseCase)))
                .authenticationEntryPoint(authenticationEntryPoint));
        return http.build();
    }
}
