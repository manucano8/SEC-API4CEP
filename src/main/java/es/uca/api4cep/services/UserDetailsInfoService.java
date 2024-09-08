package es.uca.api4cep.services;

import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import es.uca.api4cep.entities.User;
import es.uca.api4cep.entities.UserInfo;
import es.uca.api4cep.repositories.UserRepository;


@Service
public class UserDetailsInfoService implements UserDetailsService {

	private final UserRepository userRepository;

    public UserDetailsInfoService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
	
	@Override
	public UserDetails loadUserByUsername(String username) {

		Optional<User> user = userRepository.findByUsername(username);
		return user.map(UserInfo::new)
				.orElseThrow(()-> new UsernameNotFoundException("User not found"+username));
	}

}
