package grpc.demo.controller;

import grpc.demo.dto.AuthResponse;
import grpc.demo.dto.LoginRequest;
import grpc.demo.dto.RegistrationRequest;
import grpc.demo.model.User;
import grpc.demo.security.JwtUtil;
import grpc.demo.service.UserService;
import grpc.demo.service.EmailService;
import grpc.demo.util.XssProtectionUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private AuthenticationManager authenticationManager;

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/wishlist";
        }
        
        model.addAttribute("title", "Вход в систему");
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, 
                       @RequestParam String password, 
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        try {
            String cleanEmail = XssProtectionUtil.validateEmail(email);
            String cleanPassword = XssProtectionUtil.cleanAndValidate(password, 50);
            
            User user = userService.authenticateUser(cleanEmail, cleanPassword)
                    .orElseThrow(() -> new IllegalArgumentException("Неверный email или пароль"));
            
            userService.updateUserLastLogin(user.getId());

            String token = jwtUtil.generateToken(cleanEmail, user.getId(), user.getPublicId());

            session.setAttribute("currentUser", user);
            session.setAttribute("jwtToken", token);

            System.out.println("Пользователь вошел: " + user.getEmail());
            System.out.println("Сессия ID: " + session.getId());
            System.out.println("JWT токен сгенерирован");
            
            return "redirect:/wishlist";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/login";
        }
    }
    
   @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<?> loginApi(@Valid @RequestBody LoginRequest loginRequest, BindingResult bindingResult) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest()
                    .body("Ошибка валидации: " + bindingResult.getFieldError().getDefaultMessage());
            }
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = userService.getUserByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
            
            userService.updateUserLastLogin(user.getId());
            
            String token = jwtUtil.generateToken(loginRequest.getEmail(), user.getId(), user.getPublicId());
            
            AuthResponse response = new AuthResponse(
                token, 
                user.getId(), 
                user.getPublicId(), 
                user.getEmail(), 
                user.getFirstName(), 
                user.getLastName()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Неверный email или пароль");
        }
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("title", "Регистрация");
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String confirmPassword,
                          @RequestParam String firstName,
                          @RequestParam String lastName,
                          RedirectAttributes redirectAttributes) {
        try {
            String cleanEmail = XssProtectionUtil.validateEmail(email);
            String cleanPassword = XssProtectionUtil.cleanAndValidate(password, 50);
            String cleanConfirmPassword = XssProtectionUtil.cleanAndValidate(confirmPassword, 50);
            String cleanFirstName = XssProtectionUtil.validateName(firstName);
            String cleanLastName = XssProtectionUtil.validateName(lastName);
            
            if (!cleanPassword.equals(cleanConfirmPassword)) {
                throw new IllegalArgumentException("Пароли не совпадают");
            }
            
            if (cleanPassword.length() < 6) {
                throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
            }
            
            User user = userService.registerUser(cleanEmail, cleanPassword, cleanFirstName, cleanLastName);
            redirectAttributes.addFlashAttribute("success", "Регистрация прошла успешно! Теперь вы можете войти.");
            return "redirect:/auth/login";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        return "redirect:/";
    }
    
    @PostMapping("/api/logout")
    @ResponseBody
    public ResponseEntity<?> logoutApi() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok("Выход выполнен успешно");
    }

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        model.addAttribute("title", "Восстановление пароля");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, 
                                      RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                String resetToken = UUID.randomUUID().toString();
                user.setResetToken(resetToken);
                user.setResetTokenExpiry(LocalDateTime.now().plusHours(1)); // Токен действителен ЛИШЬ 1 час
                userService.updateUser(user);

                emailService.sendPasswordResetEmail(email, resetToken);
                
                redirectAttributes.addFlashAttribute("success", "Инструкции по восстановлению пароля отправлены на ваш email");
            } else {
                redirectAttributes.addFlashAttribute("success", "Если указанный email существует, инструкции по восстановлению пароля будут отправлены");
            }
            
            return "redirect:/auth/forgot-password";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при отправке письма: " + e.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model, RedirectAttributes redirectAttributes) {
        try {
            Optional<User> userOpt = userService.findByResetToken(token);
            if (userOpt.isPresent() && userOpt.get().getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                model.addAttribute("token", token);
                model.addAttribute("title", "Сброс пароля");
                return "reset-password";
            } else {
                redirectAttributes.addFlashAttribute("error", "Ссылка для сброса пароля недействительна или истекла");
                return "redirect:/auth/forgot-password";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при проверке токена");
            return "redirect:/auth/forgot-password";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                     @RequestParam String password,
                                     @RequestParam String confirmPassword,
                                     RedirectAttributes redirectAttributes) {
        try {
            if (!password.equals(confirmPassword)) {
                throw new IllegalArgumentException("Пароли не совпадают");
            }
            
            if (password.length() < 6) {
                throw new IllegalArgumentException("Пароль должен содержать минимум 6 символов");
            }
            
            Optional<User> userOpt = userService.findByResetToken(token);
            if (userOpt.isPresent() && userOpt.get().getResetTokenExpiry().isAfter(LocalDateTime.now())) {
                User user = userOpt.get();
                userService.updatePassword(user.getId(), password);

                user.setResetToken(null);
                user.setResetTokenExpiry(null);
                userService.updateUser(user);
                
                redirectAttributes.addFlashAttribute("success", "Пароль успешно изменен! Теперь вы можете войти.");
                return "redirect:/auth/login";
            } else {
                redirectAttributes.addFlashAttribute("error", "Ссылка для сброса пароля недействительна или истекла");
                return "redirect:/auth/forgot-password";
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/auth/reset-password?token=" + token;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка при сбросе пароля");
            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/test-email")
    @ResponseBody
    public String testEmail() {
        try {
            logger.info("Начало теста отправки email");
            String testToken = UUID.randomUUID().toString();
            emailService.sendPasswordResetEmail("test@example.com", testToken);
            return "Email отправлен успешно! Проверьте Mailtrap.io";
        } catch (Exception e) {
            logger.error("Ошибка при тесте отправки email", e);
            return "Ошибка: " + e.getMessage();
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}
