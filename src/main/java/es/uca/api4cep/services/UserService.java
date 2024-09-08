package es.uca.api4cep.services;

import java.util.Collections;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.uca.api4cep.entities.User;
import es.uca.api4cep.exceptions.ResourceNotFoundException;
import es.uca.api4cep.repositories.UserRepository;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	// Constructor to inject the dependencies
	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/**
	 * Creates a new user with encoded password and default role if none is provided.
	 * @param user The user entity to be created.
	 * @return The created user entity.
	 */
	public User createUser(User user) {
		if (user.getRoles() == null || user.getRoles().isEmpty()) {
			user.setRoles(Collections.singletonList("USER"));
		}
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return userRepository.save(user);
	}

	/**
	 * Retrieves all users from the repository.
	 * @return An iterable of all user entities.
	 */
	public Iterable<User> findAllUser() {
		return userRepository.findAll();
	}
	
	/**
	* Finds a user by its ID.
	* @param id The ID of the user to be found.
	* @return The user entity with the specified ID.
	* @throws ResourceNotFoundException if the user is not found.
	*/
	public User findById(Long id) {
		return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
	}

	/**
	 * Updates an existing user with new details.
	 * @param user The user entity with updated details.
	 * @param id The ID of the user to be updated.
	 * @return The updated user entity.
	 * @throws ResourceNotFoundException if the user is not found.
	 */
	public User updateUser(User user, Long id) {
		User updatedUser = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

		updatedUser.setUsername(user.getUsername());
		updatedUser.setPassword(passwordEncoder.encode(user.getPassword()));
		updatedUser.setEmail(user.getEmail());
		updatedUser.setRoles(user.getRoles());
		return userRepository.save(updatedUser);
	}

	/**
	 * Deletes a user by its ID.
	 * @param id The ID of the user to be deleted.
	 * @throws ResourceNotFoundException if the user is not found.
	 */
	public void deleteUser(Long id) {
		User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
		userRepository.delete(user);

	}
}
