package grpc.demo.service;

import grpc.demo.model.User;
import grpc.demo.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private Counter userRegistrations;
    
    @Autowired
    private Counter userLogins;
    
    @Autowired
    private Counter authenticationFailures;

    public User registerUser(String email, String password, String firstName, String lastName) {
        MDC.put("email", maskEmail(email));
        MDC.put("operation", "registration");
        
        logger.info("Начало регистрации пользователя: {}", maskEmail(email));
        
        if (userRepository.existsByEmail(email)) {
            logger.warn("Попытка регистрации с существующим email: {}", maskEmail(email));
            throw new IllegalArgumentException("Пользователь с таким email уже существует");
        }
        
        if (password.length() < 6) {
            logger.warn("Попытка регистрации с коротким паролем для email: {}", maskEmail(email));
            throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
        }
        
        try {
            User user = new User();
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setCreatedAt(LocalDateTime.now());
            user.setLastLoginAt(LocalDateTime.now());
            // publicId генерируется автоматически в конструкторе
            
            User savedUser = userRepository.save(user);
            
            // Увеличиваем счетчик регистраций
            userRegistrations.increment();
            
            // Записываем в аудит лог
            auditLogger.info("USER_REGISTERED: email={}, userId={}, publicId={}", 
                maskEmail(email), savedUser.getId(), savedUser.getPublicId());
            
            logger.info("Пользователь успешно зарегистрирован: userId={}, publicId={}", 
                savedUser.getId(), savedUser.getPublicId());
            
            return savedUser;
        } catch (Exception e) {
            logger.error("Ошибка при регистрации пользователя: {}", maskEmail(email), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    public Optional<User> authenticateUser(String email, String password) {
        MDC.put("email", maskEmail(email));
        MDC.put("operation", "authentication");
        
        logger.info("Попытка аутентификации пользователя: {}", maskEmail(email));
        
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            
            if (userOpt.isEmpty()) {
                logger.warn("Пользователь не найден: {}", maskEmail(email));
                authenticationFailures.increment();
                return Optional.empty();
            }
            
            User user = userOpt.get();
            
            if (passwordEncoder.matches(password, user.getPassword())) {
                // Успешная аутентификация
                userLogins.increment();
                updateUserLastLogin(user.getId());
                
                auditLogger.info("USER_AUTHENTICATED: email={}, userId={}, publicId={}", 
                    maskEmail(email), user.getId(), user.getPublicId());
                
                logger.info("Пользователь успешно аутентифицирован: userId={}", user.getId());
                return Optional.of(user);
            } else {
                // Неверный пароль
                logger.warn("Неверный пароль для пользователя: {}", maskEmail(email));
                authenticationFailures.increment();
                
                auditLogger.warn("AUTHENTICATION_FAILED: email={}, reason=invalid_password", 
                    maskEmail(email));
                
                return Optional.empty();
            }
        } catch (Exception e) {
            logger.error("Ошибка при аутентификации пользователя: {}", maskEmail(email), e);
            authenticationFailures.increment();
            return Optional.empty();
        } finally {
            MDC.clear();
        }
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public void updateUserLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByResetToken(String resetToken) {
        return userRepository.findByResetToken(resetToken);
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }

    public void updatePassword(Long userId, String newPassword) {
        MDC.put("userId", userId.toString());
        MDC.put("operation", "password_update");
        
        logger.info("Обновление пароля пользователя: userId={}", userId);
        
        try {
            userRepository.findById(userId).ifPresent(user -> {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                
                auditLogger.info("PASSWORD_UPDATED: userId={}, email={}", 
                    userId, maskEmail(user.getEmail()));
                
                logger.info("Пароль успешно обновлен: userId={}", userId);
            });
        } catch (Exception e) {
            logger.error("Ошибка при обновлении пароля: userId={}", userId, e);
            throw e;
        } finally {
            MDC.clear();
        }
    }
    
    /**
     * Маскирует email для логирования (защита PII данных)
     */
    private String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "unknown";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return email;
        }
        
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        
        if (localPart.length() <= 2) {
            return "**" + domain;
        }
        
        return localPart.substring(0, 2) + "***" + domain;
    }
}
