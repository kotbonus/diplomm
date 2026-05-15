import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class test_mvideo_parsing {
    
    public static void main(String[] args) {
        String url = "https://www.mvideo.ru/products/elektrogril-tefal-optigrill-gc712d34-serebristyi-20036881";
        
        // Пробуем несколько паттернов для разных форматов URL
        Pattern[] patterns = {
            Pattern.compile("/products/.+-(\\d+)"),           // Основной паттерн
            Pattern.compile("/products/(\\d+)"),               // Простой формат
            Pattern.compile("- (\\d+)$"),                      // ID в конце через дефис
            Pattern.compile("(\\d{8})$")                       // 8-значный ID в конце
        };
        
        String productId = null;
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                productId = matcher.group(1);
                System.out.println("Найден ID с паттерном " + pattern.pattern() + ": " + productId);
                break;
            }
        }
        
        if (productId == null) {
            // Если ничего не помогло, пробуем извлечь последние цифры
            Pattern lastDigitsPattern = Pattern.compile("(\\d+)(?:\\?.*)?$");
            Matcher lastMatcher = lastDigitsPattern.matcher(url);
            if (lastMatcher.find()) {
                String id = lastMatcher.group(1);
                // Проверяем, что ID похож на ID товара (обычно 6-8 цифр)
                if (id.length() >= 6 && id.length() <= 10) {
                    productId = id;
                    System.out.println("Найден ID из последних цифр: " + productId);
                }
            }
        }
        
        if (productId != null) {
            System.out.println("Извлеченный ID продукта: " + productId);
        } else {
            System.out.println("Не удалось извлечь ID продукта");
        }
    }
}
