# PowerShell скрипт для генерации зашифрованных значений конфигурации с использованием Jasypt

Write-Host "========================================"
Write-Host "Генерация зашифрованных значений Jasypt"
Write-Host "========================================"

# Проверяем наличие Java
try {
    $javaVersion = java -version 2>&1
    Write-Host "Java найдена: $($javaVersion[0])"
} catch {
    Write-Host "Ошибка: Java не найдена. Установите Java для работы скрипта." -ForegroundColor Red
    Read-Host "Нажмите Enter для выхода"
    exit 1
}

# Запрашиваем пароль для шифрования
$password = Read-Host "Введите пароль для шифрования (или нажмите Enter для генерации)"

if ([string]::IsNullOrEmpty($password)) {
    Write-Host "Генерируем случайный пароль..."
    $password = -join ((48..122) | Get-Random -Count 32 | ForEach-Object {[char]$_})
    Write-Host "Сгенерированный пароль: $password" -ForegroundColor Green
    Write-Host "Сохраните этот пароль в надежном месте!" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Шифрование значений конфигурации..."
Write-Host ""

# Функция для шифрования значения
function Encrypt-Value {
    param(
        [string]$Value,
        [string]$Password
    )
    
    try {
        $result = java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil $Password encrypt_config $Value 2>&1
        if ($LASTEXITCODE -eq 0) {
            return $result.Trim()
        } else {
            Write-Host "Ошибка шифрования значения '$Value': $result" -ForegroundColor Red
            return $null
        }
    } catch {
        Write-Host "Ошибка выполнения Java для шифрования '$Value': $_" -ForegroundColor Red
        return $null
    }
}

# Шифруем пароль базы данных
Write-Host "Шифрование пароля базы данных (password):"
$dbPassword = Encrypt-Value -Value "password" -Password $password
if ($dbPassword) {
    Write-Host "Результат: ENC($dbPassword)" -ForegroundColor Green
}

Write-Host ""

# Шифруем пароль Mailtrap
Write-Host "Шифрование пароля Mailtrap (9b651d91219bc3):"
$mailtrapPassword = Encrypt-Value -Value "9b651d91219bc3" -Password $password
if ($mailtrapPassword) {
    Write-Host "Результат: ENC($mailtrapPassword)" -ForegroundColor Green
}

Write-Host ""

# Шифруем пароль Gmail
Write-Host "Шифрование пароля Gmail (Kk4952331108):"
$gmailPassword = Encrypt-Value -Value "Kk4952331108" -Password $password
if ($gmailPassword) {
    Write-Host "Результат: ENC($gmailPassword)" -ForegroundColor Green
}

Write-Host ""

# Шифруем JWT секрет
Write-Host "Шифрование JWT секрета (mySecretKey123456789012345678901234567890):"
$jwtSecret = Encrypt-Value -Value "mySecretKey123456789012345678901234567890" -Password $password
if ($jwtSecret) {
    Write-Host "Результат: ENC($jwtSecret)" -ForegroundColor Green
}

Write-Host ""

# Генерируем ключ для бэкапов
Write-Host "Генерация ключа для шифрования бэкапов:"
try {
    $backupKey = java -cp "target/classes;target/dependency/*" grpc.demo.util.EncryptionUtil $Password generate_backup_key 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "Ключ бэкапа: $($backupKey.Trim())" -ForegroundColor Green
    } else {
        Write-Host "Ошибка генерации ключа бэкапа: $backupKey" -ForegroundColor Red
    }
} catch {
    Write-Host "Ошибка выполнения Java для генерации ключа бэкапа: $_" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================"
Write-Host "Готово! Скопируйте зашифрованные значения в application-encrypted.properties"
Write-Host "========================================"
Write-Host ""
Write-Host "Для запуска приложения с зашифрованной конфигурацией используйте:"
Write-Host "java -Djasypt.encryptor.password=$password -jar target/diplomm-1.0-SNAPSHOT.jar --spring.profiles.active=encrypted"
Write-Host ""
Write-Host "Или установите переменную окружения:"
Write-Host "`$env:JASYPT_ENCRYPTOR_PASSWORD='$password'"
Write-Host "java -jar target/diplomm-1.0-SNAPSHOT.jar --spring.profiles.active=encrypted"
Write-Host ""

Read-Host "Нажмите Enter для выхода"
