package es.uca.api4cep.entities;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.AccessMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Table(name= "TABLE_USER")
@Entity
@Getter
@Setter
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    @Schema(accessMode = AccessMode.READ_ONLY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, length = 50, unique = true)
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, length = 50, unique = true)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 64)
    private String password;
	
	private List<String> roles;
}
