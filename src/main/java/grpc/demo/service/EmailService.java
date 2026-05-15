package grpc.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = "http://localhost:8080/auth/reset-password?token=" + resetToken;
        
        logger.info("Попытка отправки email на адрес: {}", toEmail);
        logger.info("Reset link: {}", resetLink);
        logger.info("From email: {}", fromEmail);
        
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("Восстановление пароля - Wishlist App");
            
            String htmlContent = buildResetEmailTemplate(resetLink);
            helper.setText(htmlContent, true);
            
            logger.info("Отправка email...");
            mailSender.send(message);
            logger.info("Email успешно отправлен на адрес: {}", toEmail);
            
        } catch (MessagingException e) {
            logger.error("Ошибка при отправке email на адрес {}: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Ошибка при отправке email: " + e.getMessage(), e);
        }
    }

    private String buildResetEmailTemplate(String resetLink) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<title>Восстановление пароля</title>"
                + "<style>"
                + "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }"
                + ".container { max-width: 600px; margin: 0 auto; padding: 20px; }"
                + ".header { background-color: #007bff; color: white; padding: 20px; text-align: center; }"
                + ".content { padding: 20px; background-color: #f9f9f9; }"
                + ".button { display: inline-block; background-color: #007bff; color: white; padding: 12px 24px; text-decoration: none; border-radius: 4px; margin: 20px 0; }"
                + ".footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "<div class='container'>"
                + "<div class='header'>"
                + "<h1>Восстановление пароля</h1>"
                + "</div>"
                + "<div class='content'>"
                + "<p>Здравствуйте!</p>"
                + "<p>Вы запросили восстановление пароля для вашего аккаунта в приложении Wishlist.</p>"
                + "<p>Для сброса пароля, пожалуйста, перейдите по следующей ссылке:</p>"
                + "<p style='text-align: center;'>"
                + "<a href='" + resetLink + "' class='button'>Сбросить пароль</a>"
                + "</p>"
                + "<p>Если вы не запрашивали восстановление пароля, просто проигнорируйте это письмо.</p>"
                + "<p><strong>Важно:</strong> Ссылка действительна в течение 1 часа.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>Это автоматическое сообщение. Пожалуйста, не отвечайте на него.</p>"
                + "<p>&copy; 2024 Wishlist App. Все права защищены.</p>"
                + "</div>"
                + "</div>"
                + "</body>"
                + "</html>";
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
