# Используем легкий образ OpenJDK
FROM openjdk:20-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Устанавливаем Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Копируем файлы проекта
COPY pom.xml .
COPY src ./src

# Собираем приложение
RUN mvn clean package -DskipTests

# Открываем порт 8080
EXPOSE 8080

# Запускаем приложение
CMD ["java", "-jar", "target/diplomm-1.0-SNAPSHOT.jar"]
