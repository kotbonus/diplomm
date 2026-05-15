package grpc.demo;

import grpc.demo.service.PriceParsingService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MvideoPriceParsingTest {

    @Test
    public void testMvideoUrlParsing() {
        PriceParsingService parser = new PriceParsingService();
        
        // Тест URL из примера пользователя
        String testUrl = "https://www.mvideo.ru/products/elektrogril-tefal-optigrill-gc712d34-serebristyi-20036881";
        
        // Проверяем, что URL поддерживается
        assertTrue(parser.isUrlSupported(testUrl));
        
        // Пробуем распарсить цену
        PriceParsingService.PriceResult result = parser.parsePriceFromUrl(testUrl);
        
        System.out.println("Результат парсинга М.Видео:");
        System.out.println("Успех: " + result.isSuccess());
        System.out.println("Текущая цена: " + result.getPrice());
        System.out.println("Старая цена: " + result.getOldPrice());
        System.out.println("Скидка: " + result.getDiscount());
        if (!result.isSuccess()) {
            System.out.println("Ошибка: " + result.getError());
        }
        
        // Проверяем, что хотя бы цена найдена
        if (result.isSuccess()) {
            assertTrue(result.getPrice() > 0, "Цена должна быть больше 0");
        }
    }
    
    @Test
    public void testProductIdExtraction() {
        // Тест извлечения ID продукта из URL
        String url1 = "https://www.mvideo.ru/products/elektrogril-tefal-optigrill-gc712d34-serebristyi-20036881";
        String url2 = "https://www.mvideo.ru/products/smartphone-apple-iphone-15-pro-256gb-30041357";
        
        // Используем рефлексию для доступа к приватному методу
        try {
            java.lang.reflect.Method method = PriceParsingService.class.getDeclaredMethod("extractProductIdFromMvideoUrl", String.class);
            method.setAccessible(true);
            
            String id1 = (String) method.invoke(new PriceParsingService(), url1);
            String id2 = (String) method.invoke(new PriceParsingService(), url2);
            
            assertEquals("20036881", id1);
            assertEquals("30041357", id2);
            
            System.out.println("ID продукта 1: " + id1);
            System.out.println("ID продукта 2: " + id2);
            
        } catch (Exception e) {
            fail("Ошибка при тестировании извлечения ID: " + e.getMessage());
        }
    }
}
