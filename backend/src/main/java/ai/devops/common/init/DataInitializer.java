package ai.devops.common.init;

import ai.devops.modules.user.entity.UserEntity;
import ai.devops.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String defaultPassword = "DevopsAdmin2026!";
        userRepository.findByEmail("admin@devops.ai").ifPresent(user -> {
            if (!passwordEncoder.matches(defaultPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(defaultPassword));
                userRepository.save(user);
                log.info("Initialized standardized BCrypt password hash for admin@devops.ai");
            }
        });

        userRepository.findByEmail("devops@devops.ai").ifPresent(user -> {
            if (!passwordEncoder.matches(defaultPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(defaultPassword));
                userRepository.save(user);
            }
        });

        userRepository.findByEmail("viewer@devops.ai").ifPresent(user -> {
            if (!passwordEncoder.matches(defaultPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(defaultPassword));
                userRepository.save(user);
            }
        });
    }
}
