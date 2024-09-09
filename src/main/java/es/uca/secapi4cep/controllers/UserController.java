package es.uca.secapi4cep.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import es.uca.secapi4cep.dtos.CreateUserDTO;
import es.uca.secapi4cep.dtos.UserDTO;
import es.uca.secapi4cep.entities.User;
import es.uca.secapi4cep.services.JwtService;
import es.uca.secapi4cep.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Tag(name = "3. Users", description = "Users API")
@RestController
@RequestMapping("/user")
public class UserController {

	// Logger for recording logs related to user operations
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	// Services for handling user and JWT operations
	private final UserService userService;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	// Helper method to get the current username from the security context
	private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userDetails.getUsername();
        }
        return "Anonymous";  // Default to "Anonymous" if no user is authenticated
    } 

	// Constructor to inject UserService, JwtService, and AuthenticationManager
    public UserController(UserService userService, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

	@Operation(
        summary = "Creates a new user",
        description = "Creates a new user."
    )
	@ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "User successfully created"),
        @ApiResponse(responseCode = "400", description = "Invalid input provided")
    })
	@PostMapping(value= "/create", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String,String>> createUser(@RequestBody @Valid CreateUserDTO createUserDTO) {
		// Creates a new User entity based on the input CreateUserDTO and assigns it the role of USER
		User user = new User();
		user.setUsername(createUserDTO.getUsername());
		user.setEmail(createUserDTO.getEmail());
		user.setPassword(createUserDTO.getPassword());
		user.setRoles(Collections.singletonList("USER"));
		User createUser = userService.createUser(user);

		Map<String, String> response = new HashMap<>();

		if(createUser!=null) {
			logger.info("User " + createUser.getId() + " created succesfully.");
			response.put("message", "User created successfully!");
			return new ResponseEntity<>(response, HttpStatus.CREATED);
		}else {
			logger.error("Failed to create user.");
			response.put("message", "Failed to create user.");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
	
	@Operation(
		summary = "Retrieves all existing users",
		description ="Retrieves all existing users. Only admins are authorized to use this operation."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(hidden = true))),
		@ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated."),
		@ApiResponse(responseCode = "404", description = "No users found.")
	})
    @SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value= "/getAll", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<?> getAllUser(){
		// Retrieve all users from the service and map them to UserDTOs
		Iterable<User> findAllUser = userService.findAllUser();
		if(findAllUser!=null) {
			List<UserDTO> userDTOs = new ArrayList<>();
        for (User user : findAllUser) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setRoles(user.getRoles());
            userDTOs.add(userDTO);
        }
			logger.info("User " + getCurrentUsername() + " successfully retrieved all users.");
			return new ResponseEntity<>(userDTOs, HttpStatus.OK);
		}else {
			logger.error("User " + getCurrentUsername() + " failed to retrieve all users.");
			Map<String, String> response = new HashMap<>();
			response.put("message", "No users found.");
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	

	@Operation(
		summary = "Retrieves the user with the provided ID",
		description = "Retrieves the user with the provided ID. Only admins are authorized to use this operation."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", content = @Content(schema = @Schema(hidden = true))),
		@ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated."),
		@ApiResponse(responseCode = "404", description = "User not found.")
	})
    @SecurityRequirement(name = "Bearer Authentication")
	@GetMapping(value = "/getById/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<?> getUserById(@PathVariable Long id){
		// Retrieve a specific user by ID and return as DTO
		User findById = userService.findById(id);
		if(findById!=null) {
			UserDTO userDTO = new UserDTO();
            userDTO.setId(findById.getId());
            userDTO.setUsername(findById.getUsername());
            userDTO.setEmail(findById.getEmail());
            userDTO.setRoles(findById.getRoles());
			logger.info("User {} retrieved successfully by user: {}", id, getCurrentUsername());
			return new ResponseEntity<>(userDTO, HttpStatus.OK);
		}else {
			logger.warn("User with ID {} not found. Request made by user: {}", id, getCurrentUsername());
			Map<String, String> response = new HashMap<>();
            response.put("message", "User not found.");
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}
	
    @SecurityRequirement(name = "Bearer Authentication")
	@Operation(
		summary =  "Updates the user with the provided ID",
		description = "Updates the user with the provided ID. Only admins are authorized to use this operation."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "User has been successfully updated."),
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated."),
		@ApiResponse(responseCode = "400", description = "Failed to update the specified user.")
    })
	@PutMapping(value = "/update/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<?> updateUser(@RequestBody User user, @PathVariable Long id){
		// Update an existing user based on the input data
		User updateUser = userService.updateUser(user, id);
		Map<String, String> response = new HashMap<>();
		if(updateUser!=null) {
			logger.info("User with ID {} updated successfully by user: {}", id, getCurrentUsername());
			response.put("message", "User updated successfully.");
			return new ResponseEntity<>(response, HttpStatus.OK);
		}else {
			logger.error("Failed to update user with ID {}. Request made by user: {}", id, getCurrentUsername());
			response.put("message", "Failed to update user.");
			return new ResponseEntity<>(response,HttpStatus.BAD_REQUEST);
		}
	}
	
    @SecurityRequirement(name = "Bearer Authentication")
	@Operation(
		summary = "Deletes the user with the provided ID",
		description = "Deletes the user with the provided ID. Only admins are authorized to use this operation."
	)
	@ApiResponses(value = {
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated."),
		@ApiResponse(responseCode = "200", description = "User successfully deleted")
    })
	@DeleteMapping(value = "/delete/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasAuthority('ADMIN')")
	public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id){
		// Delete a user given its ID
		userService.deleteUser(id);
		logger.info("User with ID {} deleted successfully by user: {}", id, getCurrentUsername());
		Map<String, String> response = new HashMap<>();
        response.put("message", "User deleted successfully.");
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@Operation(
		summary = "Authenticates an user given its username and password",
		description = "Authenticates an user given its username and password. A JWT is returned in the response header. This JWT should be used to gain authorization in order to execute other operations."
	)
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "User successfully authenticated."),
        @ApiResponse(responseCode = "403", description = "Bad credentials. You must be properly authenticated.")
    })
	@PostMapping(value = "/authenticate", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String,String>> authenticateAndGetToken(@RequestHeader("username") String username, @RequestHeader("password") String password, HttpServletResponse response) {
		Map<String,String> responseBody = new HashMap<>();
		// Authenticate user and generate JWT token
		try {
			Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));

			if(authentication.isAuthenticated()) {
				String token = jwtService.generateToken(username);
				response.setHeader("Authorization", "Bearer " + token);
				responseBody.put("message", "Authentication successful");
				logger.info("User {} authenticated successfully.", username);
				return ResponseEntity.ok(responseBody);
			}
			else {
				responseBody.put("message", "Invalid credentials");
				logger.warn("Invalid credentials provided for user: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
			}
		} catch (AuthenticationException e) {
			responseBody.put("message", "Invalid credentials");
			logger.error("Authentication failed for user: {}.", username);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseBody);
		}
	}
}