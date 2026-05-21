package grpc.demo.config;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class JasyptConfig {

    @Value("${jasypt.encryptor.password:}")
    private String encryptorPassword;

    @Bean(name = "jasyptStringEncryptor")
    public StandardPBEStringEncryptor stringEncryptor() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        

        String password = System.getProperty("jasypt.encryptor.password");
        if (password == null) {
            password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        }
        if (password == null) {
            password = encryptorPassword;
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("Пароль для Jasypt не найден. Установите системное свойство jasypt.encryptor.password или переменную окружения JASYPT_ENCRYPTOR_PASSWORD");
        }
        
        config.setPassword(password);
        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");
        
        encryptor.setConfig(config);
        return encryptor;
    }
}
