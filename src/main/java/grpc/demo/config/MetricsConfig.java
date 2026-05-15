package grpc.demo.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MetricsConfig {

    // Счетчики для бизнес-метрик
    @Bean
    public Counter userRegistrations(MeterRegistry registry) {
        return Counter.builder("user.registrations")
                .description("Количество регистраций пользователей")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter userLogins(MeterRegistry registry) {
        return Counter.builder("user.logins")
                .description("Количество входов пользователей")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter wishlistItemsCreated(MeterRegistry registry) {
        return Counter.builder("wishlist.items.created")
                .description("Количество созданных желаний")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter wishlistItemsUpdated(MeterRegistry registry) {
        return Counter.builder("wishlist.items.updated")
                .description("Количество обновленных желаний")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter priceParsingAttempts(MeterRegistry registry) {
        return Counter.builder("price.parsing.attempts")
                .description("Количество попыток парсинга цен")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter priceParsingSuccess(MeterRegistry registry) {
        return Counter.builder("price.parsing.success")
                .description("Количество успешных парсингов цен")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter priceParsingErrors(MeterRegistry registry) {
        return Counter.builder("price.parsing.errors")
                .description("Количество ошибок парсинга цен")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter friendshipRequests(MeterRegistry registry) {
        return Counter.builder("friendship.requests")
                .description("Количество запросов в друзья")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter passwordResetRequests(MeterRegistry registry) {
        return Counter.builder("password.reset.requests")
                .description("Количество запросов на сброс пароля")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    // Таймеры для производительности
    @Bean
    public Timer priceParsingTimer(MeterRegistry registry) {
        return Timer.builder("price.parsing.duration")
                .description("Время парсинга цен")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Timer databaseQueryTimer(MeterRegistry registry) {
        return Timer.builder("database.query.duration")
                .description("Время выполнения запросов к базе данных")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Timer httpRequestTimer(MeterRegistry registry) {
        return Timer.builder("http.request.duration")
                .description("Время обработки HTTP запросов")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    // Гейджи для текущих значений
    @Bean
    public AtomicInteger activeUsers(MeterRegistry registry) {
        return registry.gauge("users.active", new AtomicInteger(0));
    }

    @Bean
    public AtomicLong totalWishlistItems(MeterRegistry registry) {
        return registry.gauge("wishlist.items.total", new AtomicLong(0));
    }

    @Bean
    public AtomicInteger priceParsingQueue(MeterRegistry registry) {
        return registry.gauge("price.parsing.queue.size", new AtomicInteger(0));
    }

    // Метрики безопасности
    @Bean
    public Counter authenticationFailures(MeterRegistry registry) {
        return Counter.builder("security.auth.failures")
                .description("Количество неудачных попыток аутентификации")
                .tag("application", "wishlist-app")
                .register(registry);
    }

    @Bean
    public Counter securityEvents(MeterRegistry registry) {
        return Counter.builder("security.events")
                .description("Количество событий безопасности")
                .tag("application", "wishlist-app")
                .register(registry);
    }
}
