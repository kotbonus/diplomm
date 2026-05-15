# Инструкции по настройке шифрования

## Быстрая настройка (5 минут)

### 1. Сборка проекта с зависимостями
```bash
mvn clean package
```

### 2. Генерация зашифрованных значений
**Windows:**
```cmd
generate-jasypt-values.bat
```

**PowerShell:**
```powershell
.\encrypt-values.ps1
```

### 3. Запуск приложения с шифрованием
```bash
java -Djasypt.encryptor.password=ВАШ_ПАРОЛЬ -jar target/diplomm-1.0-SNAPSHOT.jar --spring.profiles.active=encrypted
```

## Подробная настройка

### Шаг 1: Установка зависимостей

Зависимости уже добавлены в `pom.xml`:
- `jasypt-spring-boot-starter` - для шифрования конфигурации
- `bcprov-jdk18on` - для криптографических операций

### Шаг 2: Генерация пароля шифрования

Сгенерируйте надежный пароль:
```bash
# Используйте генератор паролей или создайте вручную
openssl rand -base64 32
```

### Шаг 3: Шифрование конфигурационных значений

Используйте утилиту EncryptionUtil:

```java
// Пример использования
String password = "your-encryption-password";
String dbPassword = EncryptionUtil.encryptForConfig("password", password);
String mailPassword = EncryptionUtil.encryptForConfig("9b651d91219bc3", password);
String jwtSecret = EncryptionUtil.encryptForConfig("mySecretKey123456789012345678901234567890", password);
```

### Шаг 4: Обновление application-encrypted.properties

Замените значения в файле:
```properties
# Пароль базы данных
spring.datasource.password=ENC(ВАШЕ_ЗАШИФРОВАННОЕ_ЗНАЧЕНИЕ)

# Пароль email
spring.mail.password=ENC(ВАШЕ_ЗАШИФРОВАННОЕ_ЗНАЧЕНИЕ)

# JWT секрет
jwt.secret=ENC(ВАШЕ_ЗАШИФРОВАННОЕ_ЗНАЧЕНИЕ)
```

### Шаг 5: Настройка переменных окружения

**Linux/macOS:**
```bash
export JASYPT_ENCRYPTOR_PASSWORD="your-encryption-password"
```

**Windows:**
```cmd
set JASYPT_ENCRYPTOR_PASSWORD=your-encryption-password
```

**Docker:**
```yaml
environment:
  - JASYPT_ENCRYPTOR_PASSWORD=your-encryption-password
```

## Проверка работы шифрования

### 1. Запуск приложения
```bash
java -jar target/diplomm-1.0-SNAPSHOT.jar --spring.profiles.active=encrypted
```

### 2. Проверка логов
При успешном запуске не должно быть ошибок Jasypt.

### 3. Проверка подключения к БД
Убедитесь, что приложение успешно подключается к базе данных.

## Управление резервными копиями

### Создание бэкапа
```bash
curl -X POST http://localhost:8080/admin/backup/create \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

### Веб-интерфейс
Перейдите в `/admin/backup` для управления бэкапами через веб-интерфейс.

## Безопасность

### ✅ Рекомендации
- Храните пароль шифрования в менеджере секретов
- Используйте разные пароли для разных окружений
- Регулярно меняйте пароль шифрования
- Создавайте резервные копии ключей бэкапов

### ❌ Запрещено
- Хранить пароль шифрования в коде
- Коммитить зашифрованные значения в Git без ключей
- Использовать слабые пароли шифрования
- Передавать пароль шифрования по незащищенным каналам

## Устранение проблем

### Ошибка: "Password not set"
**Решение:** Установите переменную окружения `JASYPT_ENCRYPTOR_PASSWORD`

### Ошибка: "Decryption operation failed"
**Решение:** Проверьте правильность пароля и формата зашифрованного значения

### Ошибка: "Backup encryption failed"
**Решение:** Убедитесь, что директория `./backups` существует и доступна для записи

## Продакшен настройка

### 1. Менеджеры секретов
```bash
# AWS Secrets Manager
export JASYPT_ENCRYPTOR_PASSWORD=$(aws secretsmanager get-secret-value --secret-id app-encryption-key --query SecretString --output text)

# HashiCorp Vault
export JASYPT_ENCRYPTOR_PASSWORD=$(vault kv get -field=password secret/app/config)
```

### 2. Kubernetes
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
type: Opaque
data:
  jasypt-password: <base64-encoded-password>
---
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      containers:
      - env:
        - name: JASYPT_ENCRYPTOR_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: jasypt-password
```

### 3. Docker Compose
```yaml
version: '3.8'
services:
  app:
    image: wishlist-app:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod,encrypted
      - JASYPT_ENCRYPTOR_PASSWORD_FILE=/run/secrets/jasypt_password
    secrets:
      - jasypt_password
secrets:
  jasypt_password:
    file: ./secrets/jasypt_password.txt
```

## Мониторинг

Следите за логами на предмет:
- Ошибок дешифрования при старте
- Неудачных операций с бэкапами
- Попыток доступа к зашифрованным данным

## Готовность к продакшену

После выполнения этих шагов ваше приложение готово к безопасной работе в продакшен среде с зашифрованной конфигурацией и резервными копиями.
