package grpc.demo.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.jasypt.iv.RandomIvGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Утилита для шифрования и дешифрования конфигурационных значений
 * и резервных копий базы данных
 */
public class EncryptionUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
    
    // Алгоритмы шифрования
    private static final String JASYPT_ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";
    private static final String BACKUP_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int AES_KEY_SIZE = 256;
    private static final int IV_SIZE = 16;
    
    /**
     * Создает encryptor для Jasypt с указанным паролем
     */
    public static StandardPBEStringEncryptor createEncryptor(String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        
        config.setPassword(password);
        config.setAlgorithm(JASYPT_ALGORITHM);
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        
        encryptor.setConfig(config);
        return encryptor;
    }
    
    /**
     * Шифрует значение для использования в application.properties
     */
    public static String encryptForConfig(String value, String password) {
        try {
            StandardPBEStringEncryptor encryptor = createEncryptor(password);
            String encrypted = encryptor.encrypt(value);
            logger.debug("Значение успешно зашифровано для конфигурации");
            return "ENC(" + encrypted + ")";
        } catch (Exception e) {
            logger.error("Ошибка при шифровании значения для конфигурации", e);
            throw new RuntimeException("Ошибка шифрования конфигурации", e);
        }
    }
    
    /**
     * Дешифрует значение из application.properties
     */
    public static String decryptFromConfig(String encryptedValue, String password) {
        try {
            if (encryptedValue == null || !encryptedValue.startsWith("ENC(") || !encryptedValue.endsWith(")")) {
                return encryptedValue; // Возвращаем как есть, если это не зашифрованное значение
            }
            
            String encrypted = encryptedValue.substring(4, encryptedValue.length() - 1);
            StandardPBEStringEncryptor encryptor = createEncryptor(password);
            String decrypted = encryptor.decrypt(encrypted);
            logger.debug("Значение успешно дешифровано из конфигурации");
            return decrypted;
        } catch (Exception e) {
            logger.error("Ошибка при дешифровании значения из конфигурации", e);
            throw new RuntimeException("Ошибка дешифрования конфигурации", e);
        }
    }
    
    /**
     * Генерирует случайный ключ AES для шифрования бэкапов
     */
    public static String generateBackupKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(AES_KEY_SIZE);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            logger.error("Ошибка при генерации ключа для бэкапа", e);
            throw new RuntimeException("Ошибка генерации ключа", e);
        }
    }
    
    /**
     * Шифрует данные бэкапа с использованием AES
     */
    public static String encryptBackup(String data, String base64Key) {
        try {
            SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
            
            // Генерируем случайный IV
            byte[] iv = new byte[IV_SIZE];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            Cipher cipher = Cipher.getInstance(BACKUP_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            
            byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            
            // Объединяем IV и зашифрованные данные
            byte[] encryptedWithIv = new byte[IV_SIZE + encryptedData.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, IV_SIZE);
            System.arraycopy(encryptedData, 0, encryptedWithIv, IV_SIZE, encryptedData.length);
            
            logger.debug("Данные бэкапа успешно зашифрованы");
            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (Exception e) {
            logger.error("Ошибка при шифровании бэкапа", e);
            throw new RuntimeException("Ошибка шифрования бэкапа", e);
        }
    }
    
    /**
     * Дешифрует данные бэкапа с использованием AES
     */
    public static String decryptBackup(String encryptedData, String base64Key) {
        try {
            SecretKey secretKey = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
            
            byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedData);
            
            // Извлекаем IV
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(encryptedWithIv, 0, iv, 0, IV_SIZE);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            // Извлекаем зашифрованные данные
            byte[] encrypted = new byte[encryptedWithIv.length - IV_SIZE];
            System.arraycopy(encryptedWithIv, IV_SIZE, encrypted, 0, encrypted.length);
            
            Cipher cipher = Cipher.getInstance(BACKUP_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            
            byte[] decryptedData = cipher.doFinal(encrypted);
            
            logger.debug("Данные бэкапа успешно дешифрованы");
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Ошибка при дешифровании бэкапа", e);
            throw new RuntimeException("Ошибка дешифрования бэкапа", e);
        }
    }
    
    /**
     * Проверяет, является ли строка зашифрованным значением Jasypt
     */
    public static boolean isEncryptedValue(String value) {
        return value != null && value.startsWith("ENC(") && value.endsWith(")");
    }
    
    /**
     * Генерирует случайный пароль для шифрования конфигурации
     */
    public static String generateConfigPassword() {
        SecureRandom random = new SecureRandom();
        byte[] passwordBytes = new byte[32]; // 256 бит
        random.nextBytes(passwordBytes);
        return Base64.getEncoder().encodeToString(passwordBytes);
    }
    
    /**
     * Main метод для запуска из командной строки
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java EncryptionUtil <password> <command> [value]");
            System.out.println("Commands:");
            System.out.println("  generate_password - generate random password");
            System.out.println("  encrypt_config <value> - encrypt value for configuration");
            System.out.println("  generate_backup_key - generate backup key");
            System.exit(1);
        }
        
        String password = args[0];
        String command = args[1];
        
        try {
            switch (command) {
                case "generate_password":
                    String generatedPassword = generateConfigPassword();
                    System.out.println(generatedPassword);
                    break;
                    
                case "encrypt_config":
                    if (args.length < 3) {
                        System.err.println("Error: value required for encryption");
                        System.exit(1);
                    }
                    if ("GENERATE".equals(password)) {
                        System.err.println("Error: Cannot use GENERATE as password for encryption");
                        System.exit(1);
                    }
                    String value = args[2];
                    String encrypted = encryptForConfig(value, password);
                    System.out.println(encrypted);
                    break;
                    
                case "generate_backup_key":
                    String backupKey = generateBackupKey();
                    System.out.println(backupKey);
                    break;
                    
                default:
                    System.err.println("Unknown command: " + command);
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
