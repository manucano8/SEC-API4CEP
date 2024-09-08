package es.uca.api4cep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

import es.uca.api4cep.services.UserDetailsInfoService;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig{

    private final UserDetailsInfoService userDetailsInfoService;

	public SecurityConfig(UserDetailsInfoService userDetailsInfoService) {
        this.userDetailsInfoService = userDetailsInfoService;
    }

    /**
     * Configures the UserDetailsService used for authentication.
     * @return A UserDetailsService that provides user details.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return userDetailsInfoService;
    }

    /**
     * Configures the AuthenticationManager used for handling user authentication.
     * @param authConfig The authentication configuration provided by Spring Security.
     * @return A configured AuthenticationManager.
     * @throws Exception If an error occurs while configuring the AuthenticationManager.
     */
    @Bean 
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Configures the AuthenticationProvider responsible for authenticating users.
     * @return A configured AuthenticationProvider with UserDetailsService and PasswordEncoder.
     */
    @Bean
	public AuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
		authenticationProvider.setPasswordEncoder(passwordEncoder());
		authenticationProvider.setUserDetailsService(userDetailsService());
		return authenticationProvider;
	}

    /**
     * Configures the PasswordEncoder used for encoding and verifying passwords.
     * @return A PasswordEncoder configured to use BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the security filter chain, including authorization, session management, and security headers.
     * @param http The HTTP security configuration.
     * @param authFilter The custom JWT authentication filter.
     * @return The configured security filter chain.
     * @throws Exception If an error occurs while configuring the security filters.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter authFilter) throws Exception {

        http
            // Disables CSRF protection to allow token-based authentication.
            .csrf(csrf -> csrf.disable())
            // Configures security headers to mitigate attacks and enhance security.
            .headers(headers -> headers
                .addHeaderWriter(new ContentSecurityPolicyHeaderWriter("default-src 'self'; script-src 'self' https://localhost:443; img-src 'self' data:; style-src 'self' https://localhost:443"))
                .addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "geolocation=(), microphone=(), camera=()"))
                .addHeaderWriter(new StaticHeadersWriter("Referrer-Policy", "no-referrer"))
                .addHeaderWriter(new StaticHeadersWriter("Expect-CT", "max-age=86400, enforce"))
                .addHeaderWriter(new StaticHeadersWriter("X-Permitted-Cross-Domain-Policies", "none"))
                .addHeaderWriter(new StaticHeadersWriter("Strict-Transport-Security", "max-age=63072000; includeSubDomains; preload"))
                .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
                .addHeaderWriter(new StaticHeadersWriter("X-Frame-Options", "DENY"))
                .addHeaderWriter(new StaticHeadersWriter("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .addHeaderWriter(new StaticHeadersWriter("Pragma", "no-cache"))
                .addHeaderWriter(new StaticHeadersWriter("Expires", "0"))
        )
             // Configures request authorization, allowing unauthenticated access to certain routes.
            .authorizeHttpRequests(authorizeHttpRequests -> authorizeHttpRequests
                .requestMatchers("/v3/api-docs/**", "/test", "/", "/webjars/**", "/swagger-resources/**", "/swagger-ui/**", "/swagger-ui.html","/user/create","/user/authenticate").permitAll()
                .anyRequest().authenticated()
            )
            // Configures exception handling using default settings.
            .exceptionHandling(Customizer.withDefaults())
            // Allows unauthenticated access to the logout functionality.
            .logout(LogoutConfigurer::permitAll)
            // Configures session management to use stateless sessions.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Requires all requests to be made over HTTPS.
            .requiresChannel(requiresChannel -> requiresChannel.anyRequest().requiresSecure())
            // Maps HTTP port 8080 to HTTPS port 443.
            .portMapper(portMapper -> portMapper.http(8080).mapsTo(443));

         // Adds the custom JWT authentication filter before the default username and password authentication filter.
        http.addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
   
}
