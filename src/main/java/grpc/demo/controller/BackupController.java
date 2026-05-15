package grpc.demo.controller;

import grpc.demo.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Контроллер для управления зашифрованными резервными копиями базы данных
 */
@Controller
@RequestMapping("/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {
    
    private static final Logger logger = LoggerFactory.getLogger(BackupController.class);
    
    @Autowired
    private BackupService backupService;
    
    /**
     * Создает новую зашифрованную резервную копию
     */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createBackup() {
        logger.info("Запрос на создание зашифрованной резервной копии от администратора");
        
        try {
            String backupFileName = backupService.createEncryptedBackup();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Резервная копия успешно создана");
            response.put("fileName", backupFileName);
            
            logger.info("Зашифрованная резервная копия успешно создана: {}", backupFileName);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Ошибка при создании резервной копии", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при создании резервной копии: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Восстанавливает базу данных из указанной резервной копии
     */
    @PostMapping("/restore/{backupFileName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreBackup(@PathVariable String backupFileName) {
        logger.info("Запрос на восстановление из резервной копии: {}", backupFileName);
        
        try {
            boolean success = backupService.restoreFromEncryptedBackup(backupFileName);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "База данных успешно восстановлена из резервной копии");
                logger.info("База данных успешно восстановлена из: {}", backupFileName);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Не удалось восстановить базу данных из резервной копии");
                logger.warn("Не удалось восстановить базу данных из: {}", backupFileName);
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при восстановлении из резервной копии: {}", backupFileName, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при восстановлении: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Получает список всех доступных резервных копий
     */
    @GetMapping("/list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> listBackups() {
        logger.info("Запрос на список резервных копий от администратора");
        
        try {
            List<String> backups = backupService.listBackups();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("backups", backups);
            response.put("count", backups.size());
            
            logger.info("Найдено {} резервных копий", backups.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Ошибка при получении списка резервных копий", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при получении списка резервных копий: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Удаляет указанную резервную копию
     */
    @DeleteMapping("/delete/{backupFileName}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBackup(@PathVariable String backupFileName) {
        logger.info("Запрос на удаление резервной копии: {}", backupFileName);
        
        try {
            boolean success = backupService.deleteBackup(backupFileName);
            
            Map<String, Object> response = new HashMap<>();
            if (success) {
                response.put("success", true);
                response.put("message", "Резервная копия успешно удалена");
                logger.info("Резервная копия успешно удалена: {}", backupFileName);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Резервная копия не найдена");
                logger.warn("Резервная копия не найдена для удаления: {}", backupFileName);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Ошибка при удалении резервной копии: {}", backupFileName, e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Ошибка при удалении резервной копии: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * Отображает страницу управления резервными копиями
     */
    @GetMapping
    public String backupManagementPage() {
        logger.info("Доступ к странице управления резервными копиями");
        return "admin/backup-management";
    }
}
