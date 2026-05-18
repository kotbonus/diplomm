package grpc.demo.service;

import grpc.demo.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Сервис для создания и управления зашифрованными резервными копиями базы данных
 */
@Service
public class BackupService {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Value("${backup.directory:./backups}")
    private String backupDirectory;
    
    @Value("${backup.encryption.key:}")
    private String backupEncryptionKey;
    
    /**
     * Создает зашифрованную резервную копию базы данных
     */
    public String createEncryptedBackup() {
        logger.info("Начало создания зашифрованной резервной копии базы данных");
        
        try {
            // Создаем директорию для бэкапов, если она не существует
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
                logger.info("Создана директория для бэкапов: {}", backupDirectory);
            }
            
            // Генерируем имя файла с временной меткой
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String backupFileName = String.format("wishlist_backup_%s.sql.enc", timestamp);
            String backupFilePath = backupPath.resolve(backupFileName).toString();
            
            // Получаем данные из всех таблиц
            StringBuilder backupData = new StringBuilder();
            backupData.append("-- Wishlist Database Backup\n");
            backupData.append("-- Created: ").append(LocalDateTime.now()).append("\n");
            backupData.append("-- Database: H2 In-Memory Database\n\n");
            
            // Добавляем данные таблицы users
            backupData.append(exportTableData("users"));
            
            // Добавляем данные таблицы friendships
            backupData.append(exportTableData("friendships"));
            
            // Добавляем данные таблицы wishlist_items
            backupData.append(exportTableData("wishlist_items"));
            
            // Шифруем данные бэкапа
            String encryptionKey = getBackupEncryptionKey();
            String encryptedData = EncryptionUtil.encryptBackup(backupData.toString(), encryptionKey);
            
            // Сохраняем зашифрованный бэкап в файл
            try (FileWriter writer = new FileWriter(backupFilePath)) {
                writer.write(encryptedData);
            }
            
            logger.info("Зашифрованная резервная копия успешно создана: {}", backupFileName);
            
            // Сохраняем ключ шифрования в отдельный файл (в реальном приложении его нужно хранить безопаснее)
            String keyFileName = backupFileName.replace(".sql.enc", ".key");
            String keyFilePath = backupPath.resolve(keyFileName).toString();
            try (FileWriter keyWriter = new FileWriter(keyFilePath)) {
                keyWriter.write(encryptionKey);
            }
            
            logger.info("Ключ шифрования сохранен в: {}", keyFileName);
            
            return backupFileName;
            
        } catch (Exception e) {
            logger.error("Ошибка при создании зашифрованной резервной копии", e);
            throw new RuntimeException("Не удалось создать резервную копию", e);
        }
    }
    
    /**
     * Экспортирует данные из указанной таблицы
     */
    private String exportTableData(String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Data for table: ").append(tableName).append("\n");
        
        try {
            // Получаем метаданные таблицы
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?",
                tableName
            );
            
            if (columns.isEmpty()) {
                sb.append("-- Table ").append(tableName).append(" is empty or doesn't exist\n\n");
                return sb.toString();
            }
            
            // Получаем все данные из таблицы
            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM " + tableName);
            
            if (rows.isEmpty()) {
                sb.append("-- Table ").append(tableName).append(" has no data\n\n");
                return sb.toString();
            }
            
            // Формируем INSERT statements
            for (Map<String, Object> row : rows) {
                StringBuilder columnsPart = new StringBuilder();
                StringBuilder valuesPart = new StringBuilder();
                
                boolean first = true;
                for (Map<String, Object> column : columns) {
                    String columnName = (String) column.get("COLUMN_NAME");
                    Object value = row.get(columnName);
                    
                    if (!first) {
                        columnsPart.append(", ");
                        valuesPart.append(", ");
                    }
                    
                    columnsPart.append(columnName);
                    
                    if (value == null) {
                        valuesPart.append("NULL");
                    } else if (value instanceof String) {
                        valuesPart.append("'").append(value.toString().replace("'", "''")).append("'");
                    } else if (value instanceof Boolean) {
                        valuesPart.append((Boolean) value ? "TRUE" : "FALSE");
                    } else {
                        valuesPart.append(value.toString());
                    }
                    
                    first = false;
                }
                
                sb.append("INSERT INTO ").append(tableName)
                  .append(" (").append(columnsPart).append(")")
                  .append(" VALUES (").append(valuesPart).append(");\n");
            }
            
            sb.append("\n");
            
        } catch (Exception e) {
            logger.warn("Ошибка при экспорте данных из таблицы {}: {}", tableName, e.getMessage());
            sb.append("-- Error exporting table ").append(tableName).append(": ").append(e.getMessage()).append("\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Восстанавливает базу данных из зашифрованной резервной копии
     */
    public boolean restoreFromEncryptedBackup(String backupFileName) {
        logger.info("Начало восстановления из зашифрованной резервной копии: {}", backupFileName);
        
        try {
            String backupFilePath = Paths.get(backupDirectory, backupFileName).toString();
            String keyFileName = backupFileName.replace(".sql.enc", ".key");
            String keyFilePath = Paths.get(backupDirectory, keyFileName).toString();
            
            // Проверяем существование файлов
            if (!Files.exists(Paths.get(backupFilePath))) {
                logger.error("Файл бэкапа не найден: {}", backupFilePath);
                return false;
            }
            
            if (!Files.exists(Paths.get(keyFilePath))) {
                logger.error("Файл ключа не найден: {}", keyFilePath);
                return false;
            }
            
            // Читаем ключ шифрования
            String encryptionKey = new String(Files.readAllBytes(Paths.get(keyFilePath)));
            
            // Читаем и дешифруем бэкап
            String encryptedData = new String(Files.readAllBytes(Paths.get(backupFilePath)));
            String decryptedData = EncryptionUtil.decryptBackup(encryptedData, encryptionKey);
            
            // Очищаем текущие данные
            clearDatabase();
            
            // Выполняем SQL команды из бэкапа
            String[] sqlStatements = decryptedData.split(";\n");
            for (String statement : sqlStatements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--")) {
                    try {
                        jdbcTemplate.execute(statement);
                    } catch (Exception e) {
                        logger.warn("Ошибка при выполнении SQL: {}", statement, e);
                    }
                }
            }
            
            logger.info("База данных успешно восстановлена из бэкапа: {}", backupFileName);
            return true;
            
        } catch (Exception e) {
            logger.error("Ошибка при восстановлении из бэкапа", e);
            return false;
        }
    }
    
    /**
     * Очищает все таблицы базы данных
     */
    private void clearDatabase() {
        logger.info("Очистка базы данных перед восстановлением");
        
        try {
            jdbcTemplate.execute("DELETE FROM wishlist_items");
            jdbcTemplate.execute("DELETE FROM friendships");
            jdbcTemplate.execute("DELETE FROM users");
            logger.info("База данных успешно очищена");
        } catch (Exception e) {
            logger.error("Ошибка при очистке базы данных", e);
            throw new RuntimeException("Не удалось очистить базу данных", e);
        }
    }
    
    /**
     * Получам ключ шифрования для бэкапов
     */
    private String getBackupEncryptionKey() {
        if (backupEncryptionKey != null && !backupEncryptionKey.isEmpty()) {
            return backupEncryptionKey;
        }
        
        // Генерируем новый ключ, если он не задан
        String newKey = EncryptionUtil.generateBackupKey();
        logger.info("Сгенерирован новый ключ шифрования для бэкапов");
        return newKey;
    }
    
    /**
     * Получает список всех резервных копий
     */
    public List<String> listBackups() {
        try {
            Path backupPath = Paths.get(backupDirectory);
            if (!Files.exists(backupPath)) {
                return List.of();
            }
            
            return Files.list(backupPath)
                    .filter(path -> path.toString().endsWith(".sql.enc"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
                    
        } catch (IOException e) {
            logger.error("Ошибка при получении списка бэкапов", e);
            return List.of();
        }
    }
    
    /**
     * Удаляет указанную резервную копию и ее ключ
     */
    public boolean deleteBackup(String backupFileName) {
        try {
            String backupFilePath = Paths.get(backupDirectory, backupFileName).toString();
            String keyFileName = backupFileName.replace(".sql.enc", ".key");
            String keyFilePath = Paths.get(backupDirectory, keyFileName).toString();
            
            boolean deleted = true;
            
            if (Files.exists(Paths.get(backupFilePath))) {
                Files.delete(Paths.get(backupFilePath));
                logger.info("Удален файл бэкапа: {}", backupFileName);
            } else {
                deleted = false;
            }
            
            if (Files.exists(Paths.get(keyFilePath))) {
                Files.delete(Paths.get(keyFilePath));
                logger.info("Удален файл ключа: {}", keyFileName);
            }
            
            return deleted;
            
        } catch (IOException e) {
            logger.error("Ошибка при удалении бэкапа: {}", backupFileName, e);
            return false;
        }
    }
}
