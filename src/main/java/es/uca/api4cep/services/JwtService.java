package es.uca.api4cep.services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
	private String secret;

    /**
     * Generates a signing key for JWT.
     * @return The SecretKey used for signing JWTs.
     */
    private SecretKey getSignKey() {
        SecretKey jwtSecret;
        jwtSecret = Keys.hmacShaKeyFor(secret.getBytes());
        return jwtSecret;
    }

    /**
     * Extracts the username from the JWT.
     * @param token The JWT token.
     * @return The username extracted from the token.
     */
	public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT.
     * @param token The JWT token.
     * @return The expiration date of the token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT.
     * @param token The JWT token.
     * @param claimsResolver A function to resolve the claim.
     * @param <T> The type of the claim.
     * @return The resolved claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from the JWT.
     * @param token The JWT token.
     * @return The claims contained in the token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if the JWT is expired.
     * @param token The JWT token.
     * @return True if the token is expired, false otherwise.
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates the JWT against the user details.
     * @param token The JWT token.
     * @param userDetails The user details to validate against.
     * @return True if the token is valid, false otherwise.
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
	
    /**
     * Generates a new JWT for the given username.
     * @param username The username for which the token is generated.
     * @return The generated JWT token.
     */
	public String generateToken(String username) {
		Map<String, Object> claims = new HashMap<>();
		return createToken(claims, username);
	}

    /**
     * Creates a JWT with the specified claims and username.
     * @param claims The claims to include in the token.
     * @param username The username for the token.
     * @return The created JWT token.
     */
	private String createToken(Map<String, Object> claims, String username) {
		return Jwts.builder()
				.claims(claims)
				.subject(username)
				.issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(System.currentTimeMillis()+1000*60*30))
				.signWith(getSignKey(), Jwts.SIG.HS512).compact();
	}
}
