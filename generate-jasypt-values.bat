@echo off
REM Скрипт для генерации зашифрованных значений конфигурации с использованием Jasypt

echo ========================================
echo Генерация зашифрованных значений Jasypt
echo ========================================

REM Проверяем наличие Java
java -version >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo Ошибка: Java не найдена. Установите Java для работы скрипта.
    pause
    exit /b 1
)

REM Запрашиваем пароль для шифрования
set /p PASSWORD="Введите пароль для шифрования (или нажмите Enter для генерации): "

if "%PASSWORD%"=="" (
    echo Генерируем случайный пароль...
    for /f "delims=" %%i in ('java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil "GENERATE" generate_password') do set PASSWORD=%%i
    echo Сгенерированный пароль: %PASSWORD%
)

echo.
echo Шифрование значений конфигурации...
echo.

REM Шифруем пароль базы данных
echo Шифрование пароля базы данных (password):
java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil %PASSWORD% encrypt_config password

REM Шифруем пароль Mailtrap
echo.
echo Шифрование пароля Mailtrap (9b651d91219bc3):
java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil %PASSWORD% encrypt_config 9b651d91219bc3

REM Шифруем пароль Gmail
echo.
echo Шифрование пароля Gmail (Kk4952331108):
java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil %PASSWORD% encrypt_config Kk4952331108

REM Шифруем JWT секрет
echo.
echo Шифрование JWT секрета (mySecretKey123456789012345678901234567890):
java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil %PASSWORD% encrypt_config mySecretKey123456789012345678901234567890

REM Генерируем ключ для бэкапов
echo.
echo Генерация ключа для шифрования бэкапов:
java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil %PASSWORD% generate_backup_key

echo.
echo ========================================
echo Готово! Скопируйте зашифрованные значения в application-encrypted.properties
echo ========================================
echo.
echo Для запуска приложения с зашифрованной конфигурацией используйте:
echo java -Djasypt.encryptor.password=%PASSWORD% -jar target/diplomm-1.0-SNAPSHOT.jar --spring.profiles.active=encrypted
echo.
pause
