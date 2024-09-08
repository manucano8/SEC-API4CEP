package es.uca.api4cep.repositories;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import es.uca.api4cep.entities.User;

/**
 * Repository interface for accessing User entities.
 */
@Repository
public interface UserRepository extends CrudRepository<User, Long>{

	/**
     * Finds a User by their username.
     * @param username The username of the User
     * @return An Optional containing the User with the given username, or empty if not found
     */
	Optional<User> findByUsername(String username);
}
