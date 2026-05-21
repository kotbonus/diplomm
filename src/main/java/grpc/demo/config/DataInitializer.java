package grpc.demo.config;

import grpc.demo.model.User;
import grpc.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {

            if (userRepository.count() == 0) {
                System.out.println("База данных пуста, добавляем тестовых пользователей...");
                

                User admin = new User();
                admin.setEmail("admin@wishlist.com");
                admin.setFirstName("Admin");
                admin.setLastName("User");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setPublicId("admin123");
                admin.setCreatedAt(LocalDateTime.now());
                admin.setLastLoginAt(LocalDateTime.now());
                userRepository.save(admin);
                
                User ivan = new User();
                ivan.setEmail("ivan@example.com");
                ivan.setFirstName("Ivan");
                ivan.setLastName("Ivanov");
                ivan.setPassword(passwordEncoder.encode("password123"));
                ivan.setPublicId("ivan123");
                ivan.setCreatedAt(LocalDateTime.now());
                ivan.setLastLoginAt(LocalDateTime.now());
                userRepository.save(ivan);
                
                User maria = new User();
                maria.setEmail("maria@example.com");
                maria.setFirstName("Maria");
                maria.setLastName("Petrova");
                maria.setPassword(passwordEncoder.encode("password123"));
                maria.setPublicId("maria123");
                maria.setCreatedAt(LocalDateTime.now());
                maria.setLastLoginAt(LocalDateTime.now());
                userRepository.save(maria);
                
                User test = new User();
                test.setEmail("test@example.com");
                test.setFirstName("Test");
                test.setLastName("User");
                test.setPassword(passwordEncoder.encode("password123"));
                test.setPublicId("test123");
                test.setCreatedAt(LocalDateTime.now());
                test.setLastLoginAt(LocalDateTime.now());
                userRepository.save(test);
                
                // Пользователи
                User kotbonus = new User();
                kotbonus.setEmail("kotbonus2233@gmail.com");
                kotbonus.setFirstName("Kot");
                kotbonus.setLastName("Bonus");
                kotbonus.setPassword(passwordEncoder.encode("password123"));
                kotbonus.setPublicId("kotbonus123");
                kotbonus.setCreatedAt(LocalDateTime.now());
                kotbonus.setLastLoginAt(LocalDateTime.now());
                userRepository.save(kotbonus);
                
                User k2528870 = new User();
                k2528870.setEmail("k2528870@gmail.com");
                k2528870.setFirstName("K2528870");
                k2528870.setLastName("User");
                k2528870.setPassword(passwordEncoder.encode("password123"));
                k2528870.setPublicId("k2528870id");
                k2528870.setCreatedAt(LocalDateTime.now());
                k2528870.setLastLoginAt(LocalDateTime.now());
                userRepository.save(k2528870);
                
                System.out.println("Тестовые пользователи успешно добавлены в базу данных");
            } else {
                System.out.println("В базе данных уже есть " + userRepository.count() + " пользователей");
            }
        };
    }
}
