package es.uca.api4cep.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import es.uca.api4cep.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
		// Retrieve the 'Authorization' header from the request
		String authHeader = request.getHeader("Authorization");
		String token = null;
		String username = null;

		// Check if the header is present and starts with "Bearer"
		if(authHeader!=null && authHeader.startsWith("Bearer ")) {
			// Extract the token from the header
			token = authHeader.substring(7);
			// Extract the username from the token
			username = jwtService.extractUsername(token);
		}
		
		// If a username is found and there is no current authentication
		if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null) {
			// Load user details using the username
			UserDetails userDetails = userDetailsService.loadUserByUsername(username);
			
			// Validate the token and set up authentication if valid
			if(jwtService.validateToken(token, userDetails)) {
				UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authToken);
			}
		}

		// Continue the filter chain
		filterChain.doFilter(request, response);
	}
}

