package booking.config;

import booking.entity.User;
import booking.enums.Role;
import booking.enums.SlotStatus;
import booking.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            userRepository.save(User.builder()
                    .username("admin")
                    .email("admin@techpulse.com")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(Role.ROLE_ADMIN))
                    .status(SlotStatus.AVAILABLE)
                    .build());
        }

        if (!userRepository.existsByUsername("user1")) {
            userRepository.save(User.builder()
                    .username("user1")
                    .email("user1@techpulse.com")
                    .password(passwordEncoder.encode("user123"))
                    .roles(Set.of(Role.ROLE_USER))
                    .status(SlotStatus.AVAILABLE)
                    .build());
        }

        if (!userRepository.existsByUsername("user2")) {
            userRepository.save(User.builder()
                    .username("user2")
                    .email("user2@techpulse.com")
                    .password(passwordEncoder.encode("user123"))
                    .roles(Set.of(Role.ROLE_USER))
                    .status(SlotStatus.AVAILABLE)
                    .build());
        }
    }
}
