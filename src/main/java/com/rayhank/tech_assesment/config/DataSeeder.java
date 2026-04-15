package com.rayhank.tech_assesment.config;

import com.rayhank.tech_assesment.entity.User;
import com.rayhank.tech_assesment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUsers();
    }

    private void seedUsers() {
        // [name, email, plainPassword]
        List<String[]> seedData = List.of(
                new String[]{"User 1", "user1@webtech.id", "password1"},
                new String[]{"User 2", "user2@webtech.id", "password2"},
                new String[]{"User 3", "user3@worldskills.org", "password3"}
        );

        for (String[] data : seedData) {
            if (userRepository.existsByEmail(data[1])) {
                continue;
            }
            User user = new User();
            user.setName(data[0]);
            user.setEmail(data[1]);
            user.setPassword(passwordEncoder.encode(data[2]));
            userRepository.save(user);
            log.info("Seeded user: {}", data[1]);
        }
    }
}
