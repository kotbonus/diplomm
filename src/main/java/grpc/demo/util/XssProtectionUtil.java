package grpc.demo.util;

import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class XssProtectionUtil {
    
    // Паттерны для обнаружения потенциальных XSS атак
    private static final Pattern[] XSS_PATTERNS = {
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("</script>", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("expression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onerror(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onclick(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onmouseover(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onfocus(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
        Pattern.compile("onblur(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    };
    
    /**
     * Очищает строку от потенциальных XSS атак
     */
    public static String stripXSS(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        
        String cleanValue = value;
        
        // Удаляем NULL символы
        cleanValue = cleanValue.replaceAll("\0", "");
        
        // Удаляем известные XSS паттерны
        for (Pattern pattern : XSS_PATTERNS) {
            cleanValue = pattern.matcher(cleanValue).replaceAll("");
        }
        
        // Дополнительная очистка HTML тегов
        cleanValue = cleanValue.replaceAll("<", "&lt;")
                               .replaceAll(">", "&gt;")
                               .replaceAll("\"", "&quot;")
                               .replaceAll("'", "&#x27;")
                               .replaceAll("/", "&#x2F;");
        
        return cleanValue;
    }
    
    /**
     * Экранирует HTML символы для безопасного отображения
     */
    public static String escapeHtml(String value) {
        if (value == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(value);
    }
    
    /**
     * Проверяет строку на наличие потенциальных XSS атак
     */
    public static boolean containsXSS(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        
        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Очищает строку и проверяет максимальную длину
     */
    public static String cleanAndValidate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        
        String cleaned = stripXSS(value.trim());
        
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        
        return cleaned;
    }
    
    /**
     * Валидация email с базовой XSS защитой
     */
    public static String validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        
        String cleaned = stripXSS(email.trim().toLowerCase());
        
        // Базовая проверка email формата
        if (!cleaned.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Некорректный формат email");
        }
        
        if (cleaned.length() > 100) {
            throw new IllegalArgumentException("Email слишком длинный");
        }
        
        return cleaned;
    }
    
    /**
     * Валидация имени пользователя
     */
    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        
        String cleaned = stripXSS(name.trim());
        
        // Разрешаем буквы, цифры, пробелы, дефисы и апострофы
        if (!cleaned.matches("^[A-Za-zА-Яа-я0-9\\s\\-']+$")) {
            throw new IllegalArgumentException("Имя содержит недопустимые символы");
        }
        
        if (cleaned.length() > 50) {
            throw new IllegalArgumentException("Имя слишком длинное");
        }
        
        return cleaned;
    }
    
    /**
     * Валидация текста описания
     */
    public static String validateDescription(String description) {
        if (description == null) {
            return null;
        }
        
        String cleaned = stripXSS(description.trim());
        
        if (cleaned.length() > 1000) {
            throw new IllegalArgumentException("Описание слишком длинное");
        }
        
        return cleaned;
    }
}
