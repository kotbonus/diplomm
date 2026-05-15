# Документация по шифрованию конфигурации и резервных копий

## Обзор

В приложении реализована комплексная система шифрования, включающая:
- Шифрование конфигурационных файлов с помощью Jasypt
- Шифрование резервных копий базы данных с использованием AES-256
- Защиту чувствительных данных в логах

## 1. Шифрование конфигурационных файлов

### 1.1. Архитектура

Используется **Jasypt (Java Simplified Encryption)** для шифрования чувствительных данных в конфигурационных файлах:

- **Алгоритм**: PBEWITHHMACSHA512ANDAES_256
- **Количество итераций**: 1000
- **Генератор соли**: RandomSaltGenerator
- **Генератор IV**: RandomIvGenerator

### 1.2. Компоненты

#### EncryptionUtil.java
Утилита для шифрования/дешифрования значений:
```java
// Шифрование значения для конфигурации
String encrypted = EncryptionUtil.encryptForConfig("password", "mySecretKey");
// Результат: ENC(abc123def456...)

// Дешифрование значения из конфигурации
String decrypted = EncryptionUtil.decryptFromConfig("ENC(abc123def456...)", "mySecretKey");
```

#### JasyptConfig.java
Конфигурация Spring для автоматической дешифрации значений в application.properties.

#### application-encrypted.properties
Файл с зашифрованными значениями конфигурации.

### 1.3. Использование

#### Шаг 1: Генерация зашифрованных значений

Используйте скрипты для генерации зашифрованных значений:

**Windows (CMD):**
```batch
generate-jasypt-values.bat
```

**PowerShell:**
```powershell
.\encrypt-values.ps1
```

**Ручное шифрование:**
```java
String password = "your-encryption-password";
String encrypted = EncryptionUtil.encryptForConfig("secret-value", password);
System.out.println("ENC(" + encrypted + ")");
```

#### Шаг 2: Настройка application-encrypted.properties

Замените открытые значения на зашифрованные:
```properties
# Было:
spring.datasource.password=password

# Стало:
spring.datasource.password=ENC(G6k2L9mX4nQ8jV7pR3tY5wE1cF2aS9dH)
```

#### Шаг 3: Запуск приложения с паролем шифрования

**Через системное свойство:**
```bash
java -Djasypt.encryptor.password=your-secret-password -jar app.jar --spring.profiles.active=encrypted
```

**Через переменную окружения:**
```bash
export JASYPT_ENCRYPTOR_PASSWORD=your-secret-password
java -jar app.jar --spring.profiles.active=encrypted
```

**Windows:**
```cmd
set JASYPT_ENCRYPTOR_PASSWORD=your-secret-password
java -jar app.jar --spring.profiles.active=encrypted
```

### 1.4. Безопасность пароля шифрования

- **Никогда не храните пароль шифрования в коде**
- **Используйте переменные окружения в продакшене**
- **Для продакшена используйте менеджеры секретов** (AWS Secrets Manager, Azure Key Vault, HashiCorp Vault)
- **Регулярно меняйте пароль шифрования**

## 2. Шифрование резервных копий базы данных

### 2.1. Архитектура

Резервные копии шифруются с использованием **AES-256 в режиме CBC**:

- **Алгоритм**: AES/CBC/PKCS5Padding
- **Размер ключа**: 256 бит
- **Размер IV**: 128 бит
- **Ключ**: Генерируется случайно для каждой инсталляции

### 2.2. Компоненты

#### BackupService.java
Сервис для создания и управления зашифрованными бэкапами:
- Создание зашифрованных бэкапов
- Восстановление из бэкапов
- Управление ключами шифрования
- Удаление бэкапов

#### BackupController.java
REST API для управления бэкапами с защитой администратора.

### 2.3. Использование

#### Создание бэкапа через API
```bash
curl -X POST http://localhost:8080/admin/backup/create \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

#### Восстановление из бэкапа
```bash
curl -X POST http://localhost:8080/admin/backup/restore/wishlist_backup_20240510_143022.sql.enc \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

#### Получение списка бэкапов
```bash
curl -X GET http://localhost:8080/admin/backup/list \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

#### Управление через веб-интерфейс
1. Перейдите в `/admin/backup`
2. Используйте панель управления для создания, восстановления и удаления бэкапов

### 2.4. Структура файлов бэкапов

```
backups/
├── wishlist_backup_20240510_143022.sql.enc  # Зашифрованный бэкап
├── wishlist_backup_20240510_143022.key      # Ключ шифрования
├── wishlist_backup_20240511_091515.sql.enc
├── wishlist_backup_20240511_091515.key
└── ...
```

### 2.5. Безопасность бэкапов

- **Ключи шифрования хранятся отдельно от бэкапов**
- **Используются криптографически стойкие алгоритмы**
- **Каждый бэкап имеет уникальный IV**
- **Доступ к бэкапам только у администраторов**

## 3. Рекомендации по безопасности

### 3.1. Продакшен среда

1. **Переменные окружения:**
   ```bash
   export JASYPT_ENCRYPTOR_PASSWORD=$(aws secretsmanager get-secret-value --secret-id db-encryption-key --query SecretString --output text)
   ```

2. **Менеджеры секретов:**
   - AWS Secrets Manager
   - Azure Key Vault
   - HashiCorp Vault
   - Google Secret Manager

3. **Хранение ключей бэкапов:**
   - Храните ключи бэкапов в отдельном защищенном месте
   - Регулярно создавайте резервные копии ключей
   - Используйте разные ключи для разных окружений

### 3.2. Разработка

1. **Используйте application.properties для разработки**
2. **Никогда не коммитьте пароли шифрования в Git**
3. **Используйте .env файлы для локальной разработки**

### 3.3. Аудит и мониторинг

1. **Логирование операций с бэкапами**
2. **Мониторинг доступа к чувствительным данным**
3. **Регулярная проверка целостности бэкапов**

## 4. Устранение неполадок

### 4.1. Проблемы с Jasypt

**Ошибка:** `IllegalStateException: Password not set`
**Решение:** Убедитесь, что установлен пароль шифрования через системное свойство или переменную окружения.

**Ошибка:** `Decryption operation failed`
**Решение:** Проверьте правильность пароля шифрования и формата зашифрованного значения.

### 4.2. Проблемы с бэкапами

**Ошибка:** `Backup file not found`
**Решение:** Убедитесь, что файл бэкапа существует в директории `./backups`.

**Ошибка:** `Invalid key format`
**Решение:** Проверьте, что ключ шифрования в формате Base64 и соответствует файлу бэкапа.

### 4.3. Производительность

- Шифрование/дешифрование конфигурации происходит при старте приложения
- Время создания бэкапа зависит от размера базы данных
- Рекомендуется создавать бэкапы в периоды низкой нагрузки

## 5. Примеры конфигурации

### 5.1. Полная конфигурация для продакшена

```properties
# application-prod.properties
spring.profiles.active=prod,encrypted

# База данных (зашифрованные значения)
spring.datasource.url=jdbc:postgresql://prod-db:5432/wishlist
spring.datasource.username=ENC(encrypted_username)
spring.datasource.password=ENC(encrypted_password)

# Email (зашифрованные значения)
spring.mail.host=smtp.gmail.com
spring.mail.username=ENC(encrypted_email)
spring.mail.password=ENC(encrypted_email_password)

# JWT (зашифрованный секрет)
jwt.secret=ENC(encrypted_jwt_secret)

# Настройки бэкапов
backup.directory=/secure/backups
backup.encryption.key=${BACKUP_ENCRYPTION_KEY}

# Jasypt настройки
jasypt.encryptor.algorithm=PBEWITHHMACSHA512ANDAES_256
jasypt.encryptor.key-obtention-iterations=10000
```

### 5.2. Docker Compose с шифрованием

```yaml
version: '3.8'
services:
  app:
    image: wishlist-app:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod,encrypted
      - JASYPT_ENCRYPTOR_PASSWORD=${JASYPT_PASSWORD}
      - BACKUP_ENCRYPTION_KEY=${BACKUP_KEY}
    volumes:
      - ./backups:/secure/backups
    depends_on:
      - db
```

## 6. Заключение

Система шифрования обеспечивает надежную защиту чувствительных данных:
- Конфигурационные файлы защищены от несанкционированного доступа
- Резервные копии зашифрованы с использованием промышленных стандартов
- Поддерживается интеграция с менеджерами секретов
- Реализован полный жизненный цикл управления ключами

При правильном использовании система обеспечивает соответствие требованиям безопасности и стандартам защиты данных.
