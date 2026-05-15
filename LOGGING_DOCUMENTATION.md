# Система мониторинга и логирования приложения Wishlist

## Обзор

В приложении реализована полноценная система мониторинга и сбора логов, соответствующая современным требованиям к продакшн-средам. Система построена на основе стека технологий: **Prometheus + Grafana + Spring Boot Actuator + Micrometer**.

## Архитектура мониторинга

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   Приложение    │───▶│   Prometheus     │───▶│     Grafana     │
│  Spring Boot    │    │  (сбор метрик)   │    │ (визуализация)  │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                        │
         ▼                        ▼
┌─────────────────┐    ┌──────────────────┐
│  Структурирован- │    │   AlertManager   │
│   ные логи      │    │  (уведомления)   │
│     JSON        │    └──────────────────┘
└─────────────────┘
```

## Собираемые метрики

### Системные метрики (автоматические)
- **HTTP запросы**: количество, время выполнения, статусы ответов
- **JVM метрики**: память, GC, потоки, загрузка CPU
- **База данных**: количество подключений, время выполнения запросов

### Бизнес-метрики (кастомные)
- `wishlist_users_registrations_total` - количество регистраций пользователей
- `wishlist_users_logins_total` - количество успешных входов
- `wishlist_items_created_total` - количество созданных желаний
- `wishlist_price_parsing_attempts_total` - попытки парсинга цен
- `wishlist_price_parsing_success_total` - успешные парсинги цен
- `wishlist_price_parsing_errors_total` - ошибки парсинга цен
- `wishlist_price_parsing_duration_seconds` - время парсинга цен
- `wishlist_users_active` - количество активных пользователей
- `wishlist_items_total` - общее количество желаний

### Health Indicators
- **База данных**: проверка доступности H2
- **Email сервис**: проверка доступности SMTP
- **Диск**: проверка доступного места

## Настройка Prometheus

### 1. Установка Prometheus

```bash
# Скачивание Prometheus
wget https://github.com/prometheus/prometheus/releases/download/v2.40.0/prometheus-2.40.0.linux-amd64.tar.gz
tar xvfz prometheus-2.40.0.linux-amd64.tar.gz
cd prometheus-2.40.0.linux-amd64
```

### 2. Конфигурация prometheus.yml

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "alert_rules.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  - job_name: 'wishlist-app'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']
```

### 3. Запуск Prometheus

```bash
./prometheus --config.file=prometheus.yml
```

## Настройка Grafana

### 1. Установка Grafana

```bash
# Ubuntu/Debian
sudo apt-get install -y apt-transport-https
sudo apt-get install -y software-properties-common wget
wget -q -O - https://apt.grafana.com/gpg.key | sudo apt-key add -
echo "deb https://apt.grafana.com stable main" | sudo tee /etc/apt/sources.list.d/grafana.list
sudo apt-get update
sudo apt-get install grafana

# Запуск
sudo systemctl start grafana-server
sudo systemctl enable grafana-server
```

### 2. Настройка источника данных

1. Открыть Grafana: http://localhost:3000 (admin/admin)
2. Configuration → Data Sources → Add data source
3. Выбрать Prometheus
4. URL: http://localhost:9090
5. Нажать Save & Test

### 3. Импорт дашбордов

#### Дашборд для Spring Boot приложений
- ID: 6756 (Spring Boot Statistics)
- ID: 4701 (JVM Dashboard)

#### Кастомный дашборд для Wishlist приложения

```json
{
  "dashboard": {
    "title": "Wishlist Application Monitoring",
    "panels": [
      {
        "title": "User Registrations",
        "type": "stat",
        "targets": [
          {
            "expr": "increase(wishlist_users_registrations_total[5m])",
            "legendFormat": "Registrations"
          }
        ]
      },
      {
        "title": "User Logins",
        "type": "stat",
        "targets": [
          {
            "expr": "increase(wishlist_users_logins_total[5m])",
            "legendFormat": "Logins"
          }
        ]
      },
      {
        "title": "Price Parsing Success Rate",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(wishlist_price_parsing_success_total[5m]) / rate(wishlist_price_parsing_attempts_total[5m]) * 100",
            "legendFormat": "Success Rate %"
          }
        ]
      },
      {
        "title": "Price Parsing Duration",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(wishlist_price_parsing_duration_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.50, rate(wishlist_price_parsing_duration_seconds_bucket[5m]))",
            "legendFormat": "50th percentile"
          }
        ]
      }
    ]
  }
}
```

## Настройка алертов

### 1. Правила алертов (alert_rules.yml)

```yaml
groups:
  - name: wishlist_alerts
    rules:
      - alert: HighErrorRate
        expr: rate(wishlist_price_parsing_errors_total[5m]) / rate(wishlist_price_parsing_attempts_total[5m]) > 0.1
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "High error rate in price parsing"
          description: "Error rate is {{ $value | humanizePercentage }} for the last 5 minutes"
          
      - alert: SlowPriceParsing
        expr: histogram_quantile(0.95, rate(wishlist_price_parsing_duration_seconds_bucket[5m])) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow price parsing detected"
          description: "95th percentile of price parsing duration is {{ $value }}s"
          
      - alert: DatabaseDown
        expr: up{job="wishlist-app"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Wishlist application is down"
          description: "Application has been down for more than 1 minute"
```

### 2. Настройка AlertManager

```yaml
global:
  smtp_smarthost: 'localhost:587'
  smtp_from: 'alerts@wishlist.com'

route:
  group_by: ['alertname']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'web.hook'

receivers:
  - name: 'web.hook'
    email_configs:
      - to: 'admin@wishlist.com'
        subject: '[Wishlist Alert] {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          {{ end }}
```

## Структурированное логирование

### Формат логов

Приложение использует структурированные логи в JSON формате для продакшн-среды:

```json
{
  "@timestamp": "2024-01-15T10:30:45.123Z",
  "level": "INFO",
  "logger_name": "grpc.demo.service.UserService",
  "message": "Successfully registered new user with ID: 123, email: a***n@example.com",
  "traceId": "abc123-def456-ghi789",
  "userId": "192.168.1.100"
}
```

### Типы логов

1. **Application logs** (`logs/wishlist-app.json`)
   - Основные события приложения
   - Ошибки и предупреждения
   - Производительность операций

2. **Error logs** (`logs/wishlist-app-error.json`)
   - Только ошибки уровня ERROR
   - Отдельный файл для быстрого анализа проблем

3. **Audit logs** (`logs/audit.json`)
   - События безопасности
   - Входы/выходы пользователей
   - Доступ к критическим ресурсам

4. **Performance logs** (`logs/wishlist-app.json`)
   - Время выполнения методов
   - Медленные запросы
   - Метрики производительности

## Безопасность логов

### Маскирование чувствительных данных

1. **Email адреса**: `user@example.com` → `u***l@example.com`
2. **Пароли**: полностью удаляются из логов
3. **Токены**: маскируются после первых 3 символов
4. **IP адреса**: могут быть анонимизированы при необходимости

### Защита от утечек данных

- Никаких персональных данных в логах
- Минимальная информация в сообщениях об ошибках
- Контроль доступа к файлам логов
- Регулярная ротация и архивирование

## Мониторинг в продакшн-среде

### Рекомендуемые настройки

1. **Интервал сбора метрик**: 10-15 секунд
2. **Хранение метрик**: 15 дней с детализацией, 90 дней агрегированных
3. **Пороги алертов**: настраиваются индивидуально
4. **Резервное копирование**: ежедневное для конфигураций и дашбордов

### Ключевые метрики для мониторинга

1. **Доступность**: uptime приложения
2. **Производительность**: время ответа, throughput
3. **Ошибки**: rate ошибок по типам
4. **Бизнес-метрики**: активность пользователей, успех операций
5. **Ресурсы**: CPU, память, диск, сеть

### Действия по алертам

1. **Critical**: немедленное уведомление, автоматические действия
2. **Warning**: уведомление в рабочее время, ручная проверка
3. **Info**: сбор статистики, периодический анализ

## Анализ и оптимизация

### Использование логов для анализа

1. **Выявление узких мест**: анализ медленных операций
2. **Отладка проблем**: трассировка запросов
3. **Анализ поведения пользователей**: паттерны использования
4. **Безопасность**: обнаружение аномальной активности

### Оптимизация на основе метрик

1. **Масштабирование**: на основе нагрузки и производительности
2. **Кэширование**: выявление частых запросов
3. **Оптимизация кода**: медленные методы и операции
4. **Улучшение UX**: анализ времени ответа

## Заключение

Реализованная система мониторинга обеспечивает полное наблюдение за состоянием приложения, собирает важные бизнес-метрики и обеспечивает быстрое обнаружение проблем. Интеграция с Prometheus и Grafana позволяет создавать мощные дашборды и настраивать своевременные алерты.

Система соответствует требованиям безопасности к сбору и хранению логов, обеспечивает маскирование чувствительных данных и предоставляет все необходимые инструменты для анализа и оптимизации работы приложения в продакшн-среде.
