package grpc.demo.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PriceParsingService {
    
    private static final Logger logger = LoggerFactory.getLogger(PriceParsingService.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");
    
    @Autowired
    private Counter priceParsingAttempts;
    
    @Autowired
    private Counter priceParsingSuccess;
    
    @Autowired
    private Counter priceParsingErrors;
    
    @Autowired
    private Timer priceParsingTimer;

    public static class PriceResult {
        private final double price;
        private final double oldPrice;
        private final double discount;
        private final boolean success;
        private final String error;

        public PriceResult(double price, double oldPrice, double discount, boolean success, String error) {
            this.price = price;
            this.oldPrice = oldPrice;
            this.discount = discount;
            this.success = success;
            this.error = error;
        }

        public static PriceResult success(double price) {
            return new PriceResult(price, 0.0, 0.0, true, null);
        }
        
        public static PriceResult success(double price, double oldPrice, double discount) {
            return new PriceResult(price, oldPrice, discount, true, null);
        }

        public static PriceResult error(String error) {
            return new PriceResult(0.0, 0.0, 0.0, false, error);
        }

        public double getPrice() { return price; }
        public double getOldPrice() { return oldPrice; }
        public double getDiscount() { return discount; }
        public boolean isSuccess() { return success; }
        public String getError() { return error; }
    }

    public PriceResult parsePriceFromUrl(String url) {
        MDC.put("url", maskUrl(url));
        MDC.put("operation", "price_parsing");
        
        Timer.Sample sample = Timer.start();
        priceParsingAttempts.increment();
        
        logger.info("Начало парсинга цены для URL: {}", maskUrl(url));
        
        try {
            // Особая обработка для М.Видео - используем API
            if (url.contains("mvideo.ru")) {
                logger.debug("Используем API для М.Видео");
                return parseMvideoPriceFromApi(url);
            }
            
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get();

            PriceResult result;
            if (url.contains("dvamyacha.ru")) {
                logger.debug("Используем специализированный парсер для dvamyacha.ru");
                result = extractDvamyachaPriceFull(doc);
            } else {
                logger.debug("Используем универсальный парсер");
                double price = extractPrice(doc, url);
                if (price > 0) {
                    result = PriceResult.success(price);
                } else {
                    result = PriceResult.error("Цена не найдена на странице");
                }
            }
            
            if (result.isSuccess()) {
                priceParsingSuccess.increment();
                performanceLogger.info("PRICE_PARSING_SUCCESS: url={}, price={}, duration_ms={}", 
                    maskUrl(url), result.getPrice(), sample.stop(priceParsingTimer));
                logger.info("Цена успешно получена: {} руб. для URL: {}", result.getPrice(), maskUrl(url));
            } else {
                priceParsingErrors.increment();
                logger.warn("Ошибка парсинга цены: {} для URL: {}", result.getError(), maskUrl(url));
            }
            
            return result;

        } catch (IOException e) {
            priceParsingErrors.increment();
            sample.stop(priceParsingTimer);
            logger.error("Ошибка загрузки страницы: {} для URL: {}", e.getMessage(), maskUrl(url), e);
            return PriceResult.error("Не удалось загрузить страницу: " + e.getMessage());
        } catch (Exception e) {
            priceParsingErrors.increment();
            sample.stop(priceParsingTimer);
            logger.error("Ошибка при парсинге цены: {} для URL: {}", e.getMessage(), maskUrl(url), e);
            return PriceResult.error("Ошибка при парсинге цены: " + e.getMessage());
        } finally {
            MDC.clear();
        }
    }
    
    // Парсинг цены М.Видео через API
    private PriceResult parseMvideoPriceFromApi(String url) {
        try {
            // Извлекаем ID продукта из URL
            String productId = extractProductIdFromMvideoUrl(url);
            if (productId == null) {
                return PriceResult.error("Не удалось извлечь ID продукта из URL");
            }
            
            // Пробуем несколько API эндпоинтов
            String[] apiEndpoints = {
                "https://www.mvideo.ru/bff/products/prices?productIds=" + productId,
                "https://www.mvideo.ru/bff/product-details/" + productId + "/price",
                "https://www.mvideo.ru/api/product/price/" + productId,
                "https://api.mvideo.ru/products/" + productId + "/price"
            };
            
            for (String apiUrl : apiEndpoints) {
                try {
                    // Делаем запрос к API с расширенными заголовками
                    Document apiDoc = Jsoup.connect(apiUrl)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            .header("Accept", "application/json, text/plain, */*")
                            .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                            .header("Accept-Encoding", "gzip, deflate, br")
                            .header("Cache-Control", "no-cache")
                            .header("Pragma", "no-cache")
                            .header("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
                            .header("Sec-Ch-Ua-Mobile", "?0")
                            .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                            .header("Sec-Fetch-Dest", "empty")
                            .header("Sec-Fetch-Mode", "cors")
                            .header("Sec-Fetch-Site", "same-origin")
                            .ignoreContentType(true)
                            .timeout(15000)
                            .get();
                    
                    String jsonResponse = apiDoc.body().text();
                    
                    // Проверяем, что ответ не пустой и содержит данные
                    if (jsonResponse != null && !jsonResponse.trim().isEmpty() && jsonResponse.length() > 10) {
                        PriceResult result = parseMvideoApiResponse(jsonResponse);
                        if (result.isSuccess()) {
                            return result;
                        }
                    }
                } catch (Exception e) {
                    // Продолжаем со следующим эндпоинтом
                    continue;
                }
            }
            
            // Если API не сработало, пробуем стандартный метод с улучшенными селекторами
            return parseMvideoPriceFromHtml(url);
            
        } catch (Exception e) {
            return PriceResult.error("Ошибка при парсинге М.Видео: " + e.getMessage());
        }
    }
    
    // Парсинг М.Видео из HTML с улучшенными методами
    private PriceResult parseMvideoPriceFromHtml(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                    .header("Accept-Language", "ru-RU,ru;q=0.9,en;q=0.8")
                    .header("Accept-Encoding", "gzip, deflate, br")
                    .header("Cache-Control", "no-cache")
                    .header("Pragma", "no-cache")
                    .timeout(15000)
                    .get();
            
            // Пробуем сначала улучшенные селекторы
            String[] enhancedSelectors = {
                ".price-block__final-price",
                ".price__main",
                "[data-product-price]",
                ".price",
                "[class*='price']",
                ".product-price",
                ".final-price",
                ".current-price",
                ".product-card-price__current",
                ".price-current",
                "[data-testid='price']",
                "[data-test='price']",
                ".goods-price",
                ".offer-price",
                ".sale-price",
                ".actual-price"
            };
            
            double price = findPriceWithMultipleSelectors(doc, enhancedSelectors);
            if (price > 0) {
                // Пробуем найти старую цену и скидку
                double oldPrice = findOldPriceInMvideo(doc);
                double discount = (oldPrice > price) ? (oldPrice - price) : 0;
                return PriceResult.success(price, oldPrice, discount);
            }
            
            // Если стандартные селекторы не сработали, пробуем JSON-LD
            double jsonLdPrice = extractMvideoPriceFromJsonLd(doc);
            if (jsonLdPrice > 0) {
                return PriceResult.success(jsonLdPrice);
            }
            
            // Пробуем эвристический поиск
            double heuristicPrice = extractMvideoPriceHeuristic(doc);
            if (heuristicPrice > 0) {
                return PriceResult.success(heuristicPrice);
            }
            
            return PriceResult.error("Не удалось найти цену на странице М.Видео");
            
        } catch (Exception e) {
            return PriceResult.error("Ошибка при загрузке страницы М.Видео: " + e.getMessage());
        }
    }
    
    // Поиск старой цены на М.Видео
    private double findOldPriceInMvideo(Document doc) {
        String[] oldPriceSelectors = {
            ".price-block__old-price",
            ".price__old",
            "[data-old-price]",
            ".old-price",
            ".previous-price",
            ".original-price",
            ".price-before-discount"
        };
        
        return findPriceWithMultipleSelectors(doc, oldPriceSelectors);
    }
    
    // Эвристический поиск цены М.Видео
    private double extractMvideoPriceHeuristic(Document doc) {
        // Ищем элементы с текстовыми паттернами цен
        String[] pricePatterns = {
            "\\d+[\\s\\.,]*\\d{0,3}\\s*₽",
            "\\d+[\\s\\.,]*\\d{0,3}\\s*руб",
            "\\d+[\\s\\.,]*\\d{0,3}\\s*р\\.",
            "₽\\s*\\d+",
            "руб\\s*\\d+",
            "р\\.\\s*\\d+"
        };
        
        // Ищем в элементах которые могут содержать цену
        String[] possiblePriceElements = {
            "[class*='price']",
            "[class*='cost']",
            "[class*='sum']",
            "[data-price]",
            "[itemprop='price']",
            ".price",
            ".cost",
            ".sum"
        };
        
        for (String elementSelector : possiblePriceElements) {
            Elements elements = doc.select(elementSelector);
            for (Element element : elements) {
                String text = element.text().trim();
                if (text.length() < 100 && text.matches(".*\\d+.*")) {
                    for (String pattern : pricePatterns) {
                        if (text.matches(".*" + pattern + ".*")) {
                            try {
                                return parsePriceString(text);
                            } catch (Exception e) {
                                continue;
                            }
                        }
                    }
                }
            }
        }
        
        return 0.0;
    }
    
    // Извлечение ID продукта из URL М.Видео
    private String extractProductIdFromMvideoUrl(String url) {
        try {
            // URL М.Видео: https://www.mvideo.ru/products/elektrogril-tefal-optigrill-gc712d34-serebristyi-20036881
            // Пробуем несколько паттернов для разных форматов URL
            Pattern[] patterns = {
                Pattern.compile("/products/.+-(\\d+)"),           // Основной паттерн
                Pattern.compile("/products/(\\d+)"),               // Простой формат
                Pattern.compile("-(\\d+)$"),                      // ID в конце через дефис
                Pattern.compile("(\\d{8})$")                       // 8-значный ID в конце
            };
            
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
            
            // Если ничего не помогло, пробуем извлечь последние цифры
            Pattern lastDigitsPattern = Pattern.compile("(\\d+)(?:\\?.*)?$");
            Matcher lastMatcher = lastDigitsPattern.matcher(url);
            if (lastMatcher.find()) {
                String id = lastMatcher.group(1);
                // Проверяем, что ID похож на ID товара (обычно 6-8 цифр)
                if (id.length() >= 6 && id.length() <= 10) {
                    return id;
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    // Парсинг ответа от API М.Видео
    private PriceResult parseMvideoApiResponse(String jsonResponse) {
        try {
            // Пробуем несколько паттернов для извлечения цены
            Pattern[] pricePatterns = {
                Pattern.compile("\"price\"\\s*:\\s*([0-9]+)"),                    // Простой формат
                Pattern.compile("\"price\"\\s*:\\s*\"?([0-9]+)\"?"),           // С кавычками
                Pattern.compile("\"currentPrice\"\\s*:\\s*([0-9]+)"),          // Альтернативное поле
                Pattern.compile("\"basePrice\"\\s*:\\s*([0-9]+)"),             // Базовая цена
                Pattern.compile("\"value\"\\s*:\\s*([0-9]+)")                  // Значение
            };
            
            double currentPrice = 0;
            for (Pattern pattern : pricePatterns) {
                Matcher matcher = pattern.matcher(jsonResponse);
                if (matcher.find()) {
                    try {
                        currentPrice = Double.parseDouble(matcher.group(1));
                        break;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            
            if (currentPrice == 0) {
                return PriceResult.error("Цена не найдена в ответе API");
            }
            
            // Ищем старую цену
            Pattern[] oldPricePatterns = {
                Pattern.compile("\"oldPrice\"\\s*:\\s*([0-9]+)"),
                Pattern.compile("\"oldPrice\"\\s*:\\s*\"?([0-9]+)\"?"),
                Pattern.compile("\"previousPrice\"\\s*:\\s*([0-9]+)"),
                Pattern.compile("\"originalPrice\"\\s*:\\s*([0-9]+)")
            };
            
            double oldPrice = 0;
            for (Pattern pattern : oldPricePatterns) {
                Matcher matcher = pattern.matcher(jsonResponse);
                if (matcher.find()) {
                    try {
                        oldPrice = Double.parseDouble(matcher.group(1));
                        break;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            
            // Ищем скидку
            Pattern[] discountPatterns = {
                Pattern.compile("\"discount\"\\s*:\\s*([0-9]+)"),
                Pattern.compile("\"discount\"\\s*:\\s*\"?([0-9]+)\"?"),
                Pattern.compile("\"discountAmount\"\\s*:\\s*([0-9]+)"),
                Pattern.compile("\"discountValue\"\\s*:\\s*([0-9]+)")
            };
            
            double discount = 0;
            for (Pattern pattern : discountPatterns) {
                Matcher matcher = pattern.matcher(jsonResponse);
                if (matcher.find()) {
                    try {
                        discount = Double.parseDouble(matcher.group(1));
                        break;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
            
            // Рассчитываем скидку если не нашли явно
            if (discount == 0 && oldPrice > currentPrice) {
                discount = oldPrice - currentPrice;
            }
            
            return PriceResult.success(currentPrice, oldPrice, discount);
            
        } catch (Exception e) {
            return PriceResult.error("Ошибка при парсинге ответа API: " + e.getMessage());
        }
    }

    private double extractPrice(Document doc, String url) {
        double price = 0.0;

        if (url.contains("ozon.ru")) {
            price = extractOzonPrice(doc);
        } else if (url.contains("wildberries.ru")) {
            price = extractWildberriesPrice(doc);
        } else if (url.contains("aliexpress.com")) {
            price = extractAliexpressPrice(doc);
        } else if (url.contains("yandex.market")) {
            price = extractYandexMarketPrice(doc);
        } else if (url.contains("dvamyacha.ru")) {
            price = extractDvamyachaPrice(doc);
        } else if (url.contains("lamoda.ru")) {
            price = extractLamodaPrice(doc);
        } else if (url.contains("megamarket.ru")) {
            price = extractMegamarketPrice(doc);
        } else if (url.contains("dns-shop.ru")) {
            price = extractDnsPrice(doc);
        } else if (url.contains("citilink.ru")) {
            price = extractCitilinkPrice(doc);
        } else if (url.contains("mvideo.ru")) {
            price = extractMvideoPrice(doc);
        } else if (url.contains("eldorado.ru")) {
            price = extractEldoradoPrice(doc);
        } else if (url.contains("technopark.ru")) {
            price = extractTechnoparkPrice(doc);
        } else if (url.contains("pult.ru")) {
            price = extractPultPrice(doc);
        } else if (url.contains("onlinetrade.ru")) {
            price = extractOnlinetradePrice(doc);
        } else if (url.contains("holodilnik.ru")) {
            price = extractHolodilnikPrice(doc);
        } else if (url.contains("perekrestok.ru")) {
            price = extractPerekrestokPrice(doc);
        } else if (url.contains("auchan.ru")) {
            price = extractAuchanPrice(doc);
        } else if (url.contains("delivery-club.ru")) {
            price = extractDeliveryClubPrice(doc);
        } else if (url.contains("samokat.ru")) {
            price = extractSamokatPrice(doc);
        } else if (url.contains("ozon.travel")) {
            price = extractOzonTravelPrice(doc);
        } else if (url.contains("aviasales.ru")) {
            price = extractAviasalesPrice(doc);
        } else if (url.contains("cdek.ru")) {
            price = extractCdekPrice(doc);
        } else if (url.contains("boxberry.ru")) {
            price = extractBoxberryPrice(doc);
        } else if (url.contains("beru.ru")) {
            price = extractBeruPrice(doc);
        } else if (url.contains("yandex.market")) {
            price = extractYandexMarketPrice(doc);
        } else if (url.contains("market.yandex.ru")) {
            price = extractYandexMarketPrice(doc);
        } else if (url.contains("goods.ru")) {
            price = extractGoodsPrice(doc);
        } else if (url.contains("alltime.ru")) {
            price = extractAlltimePrice(doc);
        } else if (url.contains("sharmedia.ru")) {
            price = extractSharmediaPrice(doc);
        } else if (url.contains("kant.ru")) {
            price = extractKantPrice(doc);
        } else if (url.contains("labirint.ru")) {
            price = extractLabirintPrice(doc);
        } else if (url.contains("book24.ru")) {
            price = extractBook24Price(doc);
        } else if (url.contains("chitai-gorod.ru")) {
            price = extractChitaiGorodPrice(doc);
        } else if (url.contains("litres.ru")) {
            price = extractLitresPrice(doc);
        } else if (url.contains("my-shop.ru")) {
            price = extractMyshopPrice(doc);
        } else if (url.contains("oldi.ru")) {
            price = extractOldiPrice(doc);
        } else if (url.contains("pleer.ru")) {
            price = extractPleerPrice(doc);
        } else if (url.contains("drhead.ru")) {
            price = extractDrheadPrice(doc);
        } else if (url.contains("fotosklad.ru")) {
            price = extractFotoskladPrice(doc);
        } else if (url.contains("prophotos.ru")) {
            price = extractProphotosPrice(doc);
        } else {
            price = extractUniversalPrice(doc);
        }

        return price;
    }

    private double extractOzonPrice(Document doc) {
        Elements priceElements = doc.select("[data-widget='webPrice'], .price, .price-value, [class*='price']");
        return findPriceInElements(priceElements);
    }

    private double extractWildberriesPrice(Document doc) {
        Elements priceElements = doc.select(".price-block, .price, [class*='price'], .final-price");
        return findPriceInElements(priceElements);
    }

    private double extractAliexpressPrice(Document doc) {
        Elements priceElements = doc.select(".price, .price-current, [class*='price'], .product-price");
        return findPriceInElements(priceElements);
    }

    private double extractYandexMarketPrice(Document doc) {
        Elements priceElements = doc.select(".price, .price-value, [class*='price']");
        return findPriceInElements(priceElements);
    }

    private double extractDvamyachaPrice(Document doc) {
        // Сначала ищем текущую цену (приоритет)
        Elements currentPriceElements = doc.select(".product-item-detail-price-current");
        double currentPrice = findPriceInElements(currentPriceElements);
        if (currentPrice > 0) {
            return currentPrice;
        }
        
        // Если текущую не нашли, ищем любую цену с приоритетом на старую цену
        Elements oldPriceElements = doc.select(".product-item-detail-price-old");
        double oldPrice = findPriceInElements(oldPriceElements);
        if (oldPrice > 0) {
            return oldPrice;
        }
        
        // Ищем в микроразметке schema.org
        Elements schemaPriceElements = doc.select("[itemprop='price']");
        for (Element element : schemaPriceElements) {
            String priceStr = element.attr("content");
            if (!priceStr.isEmpty()) {
                try {
                    return Double.parseDouble(priceStr);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        
        // Общий поиск ценовых элементов
        Elements priceElements = doc.select("[class*='price'], [class*='cost'], [class*='amount']");
        return findPriceInElements(priceElements);
    }
    
    private PriceResult extractDvamyachaPriceFull(Document doc) {
        // Извлекаем текущую цену
        Elements currentPriceElements = doc.select(".product-item-detail-price-current");
        double currentPrice = findPriceInElements(currentPriceElements);
        
        // Извлекаем старую цену
        Elements oldPriceElements = doc.select(".product-item-detail-price-old");
        double oldPrice = findPriceInElements(oldPriceElements);
        
        // Извлекаем скидку
        Elements discountElements = doc.select(".product-item-detail-economy-price");
        double discount = findPriceInElements(discountElements);
        
        // Если не нашли текущую цену, пробуем другие методы
        if (currentPrice == 0) {
            // Ищем в микроразметке schema.org
            Elements schemaPriceElements = doc.select("[itemprop='price']");
            for (Element element : schemaPriceElements) {
                String priceStr = element.attr("content");
                if (!priceStr.isEmpty()) {
                    try {
                        currentPrice = Double.parseDouble(priceStr);
                        break;
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        }
        
        // Если все еще не нашли цену, используем общий поиск
        if (currentPrice == 0) {
            Elements priceElements = doc.select("[class*='price'], [class*='cost'], [class*='amount']");
            currentPrice = findPriceInElements(priceElements);
        }
        
        if (currentPrice > 0) {
            // Рассчитываем скидку, если не нашли явно
            if (discount == 0 && oldPrice > currentPrice) {
                discount = oldPrice - currentPrice;
            }
            return PriceResult.success(currentPrice, oldPrice, discount);
        } else {
            return PriceResult.error("Цена не найдена на странице");
        }
    }

    private double extractGenericPrice(Document doc) {
        String[] priceSelectors = {
            "[class*='price']",
            "[class*='cost']",
            "[class*='amount']",
            ".price",
            ".cost",
            ".amount",
            "[data-price]",
            "[itemprop='price']"
        };

        for (String selector : priceSelectors) {
            Elements elements = doc.select(selector);
            double price = findPriceInElements(elements);
            if (price > 0) {
                return price;
            }
        }

        return 0.0;
    }

    // Универсальный метод парсинга с эвристиками
    private double extractUniversalPrice(Document doc) {
        // 1. Микроразметка Schema.org
        double schemaPrice = extractSchemaPrice(doc);
        if (schemaPrice > 0) return schemaPrice;

        // 2. JSON-LD структурированные данные
        double jsonLdPrice = extractJsonLdPrice(doc);
        if (jsonLdPrice > 0) return jsonLdPrice;

        // 3. Мета-теги
        double metaPrice = extractMetaPrice(doc);
        if (metaPrice > 0) return metaPrice;

        // 4. Расширенные селекторы цен
        double extendedPrice = extractExtendedPriceSelectors(doc);
        if (extendedPrice > 0) return extendedPrice;

        // 5. Эвристический поиск по текстовым паттернам
        double heuristicPrice = extractHeuristicPrice(doc);
        if (heuristicPrice > 0) return heuristicPrice;

        return 0.0;
    }

    // Извлечение цены из микроразметки Schema.org
    private double extractSchemaPrice(Document doc) {
        Elements priceElements = doc.select("[itemprop='price'], [property='price'], [data-price]");
        for (Element element : priceElements) {
            String priceStr = element.attr("content")
                    .isEmpty() ? element.attr("data-price")
                    : element.attr("content");
            
            if (!priceStr.isEmpty()) {
                try {
                    return parsePriceString(priceStr);
                } catch (Exception e) {
                    continue;
                }
            }
        }
        return 0.0;
    }

    // Извлечение цены из JSON-LD
    private double extractJsonLdPrice(Document doc) {
        Elements scripts = doc.select("script[type='application/ld+json']");
        for (Element script : scripts) {
            String json = script.html();
            try {
                // Простое извлечение цены из JSON
                if (json.contains("\"price\"")) {
                    Pattern pricePattern = Pattern.compile("\"price\"\\s*:\\s*\"?([^\",}]+)\"?");
                    Matcher matcher = pricePattern.matcher(json);
                    if (matcher.find()) {
                        return parsePriceString(matcher.group(1));
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return 0.0;
    }

    // Извлечение цены из мета-тегов
    private double extractMetaPrice(Document doc) {
        String[] metaSelectors = {
            "meta[name='price']",
            "meta[property='price']",
            "meta[property='product:price:amount']",
            "meta[name='twitter:data1']",
            "meta[property='og:price:amount']"
        };

        for (String selector : metaSelectors) {
            Elements elements = doc.select(selector);
            for (Element element : elements) {
                String priceStr = element.attr("content");
                if (!priceStr.isEmpty()) {
                    try {
                        return parsePriceString(priceStr);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        return 0.0;
    }

    // Расширенные селекторы цен
    private double extractExtendedPriceSelectors(Document doc) {
        String[][] selectors = {
            // Основные ценовые селекторы
            {"[class*='price']", "[class*='cost']", "[class*='amount']", "[class*='sum']"},
            // Конкретные классы цен
            {".price", ".price-current", ".price-actual", ".price-now", ".price-final"},
            {".cost", ".cost-actual", ".cost-now", ".cost-final"},
            {".amount", ".sum", ".total", ".value"},
            // Селекторы с data-атрибутами
            {"[data-price]", "[data-cost]", "[data-amount]", "[data-value]"},
            // Специфичные для маркетплейсов
            {".product-price", ".item-price", ".goods-price", ".offer-price"},
            {".final-price", ".current-price", ".actual-price", ".sale-price"},
            // Русскоязычные селекторы
            {"[class*='цена']", "[class*='стоимость']", "[class*='сумма']"},
            {".цена", ".стоимость", ".сумма", ".итого"}
        };

        for (String[] selectorGroup : selectors) {
            for (String selector : selectorGroup) {
                Elements elements = doc.select(selector);
                double price = findPriceInElements(elements);
                if (price > 0) {
                    return price;
                }
            }
        }
        return 0.0;
    }

    // Эвристический поиск цены
    private double extractHeuristicPrice(Document doc) {
        // Ищем элементы с текстовыми паттернами цен
        String[] pricePatterns = {
            "\\d+[\\s\\.,]*\\d{0,3}\\s*₽",
            "\\d+[\\s\\.,]*\\d{0,3}\\s*руб",
            "\\d+[\\s\\.,]*\\d{0,3}\\s*р\\.",
            "\\d+[\\s\\.,]*\\d{0,3}\\s*RUB",
            "₽\\s*\\d+",
            "руб\\s*\\d+",
            "р\\.\\s*\\d+"
        };

        for (String pattern : pricePatterns) {
            Elements elements = doc.select("body *:not(script):not(style):not(noscript)");
            for (Element element : elements) {
                String text = element.text().trim();
                if (text.length() < 100 && text.matches(".*" + pattern + ".*")) {
                    try {
                        return parsePriceString(text);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }
        return 0.0;
    }

    // Универсальный парсер строки с ценой
    private double parsePriceString(String priceStr) {
        if (priceStr == null || priceStr.trim().isEmpty()) {
            return 0.0;
        }

        // Очистка строки
        priceStr = priceStr.replace("&nbsp;", " ")
                          .replace("&#8381;", "₽")
                          .replace("&#x20bd;", "₽")
                          .replace("\u00A0", " ")
                          .replace("\u2009", " ")
                          .replace("\u202F", " ")
                          .replace("\u2007", " ");

        // Поиск ценового паттерна
        Pattern pricePattern = Pattern.compile("(\\d{1,3}(?:[\\s\\.,]\\d{3})*(?:[\\.,]\\d{2})?)");
        Matcher matcher = pricePattern.matcher(priceStr);

        if (matcher.find()) {
            String cleanPrice = matcher.group(1).replaceAll("[\\s\\.,]", "");
            try {
                return Double.parseDouble(cleanPrice);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private double findPriceInElements(Elements elements) {
        Pattern pricePattern = Pattern.compile("(\\d{1,3}(?:[\\s\\.,]\\d{3})*(?:[\\.,]\\d{2})?)");
        
        for (Element element : elements) {
            String text = element.text();
            // Декодируем HTML entities
            text = text.replace("&nbsp;", " ")
                       .replace("&#8381;", "₽")
                       .replace("&#x20bd;", "₽");
            
            if (text.toLowerCase().contains("₽") || 
                text.toLowerCase().contains("rub") || 
                text.toLowerCase().contains("р.") ||
                text.contains("₽") ||
                text.matches(".*\\d+.*")) {
                
                Matcher matcher = pricePattern.matcher(text);
                if (matcher.find()) {
                    String priceStr = matcher.group(1).replaceAll("[\\s\\.,]", "");
                    try {
                        return Double.parseDouble(priceStr);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                }
            }
        }
        
        return 0.0;
    }

    // Lamoda
    private double extractLamodaPrice(Document doc) {
        String[] selectors = {
            ".product-overview__price .product-overview__price--current",
            ".price__current",
            "[data-testid='product-price']",
            ".product-price__current",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // СберМегаМаркет
    private double extractMegamarketPrice(Document doc) {
        String[] selectors = {
            ".product-price",
            ".price-block__final-price",
            "[data-testid='price']",
            ".final-price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // DNS
    private double extractDnsPrice(Document doc) {
        String[] selectors = {
            ".product-card-price__current",
            ".price-current",
            "[data-price-value]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Ситилинк
    private double extractCitilinkPrice(Document doc) {
        String[] selectors = {
            ".ProductCardVerticalPrice__current",
            ".ProductPrice__value",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // М.Видео
    private double extractMvideoPrice(Document doc) {
        // М.Видео использует SPA, поэтому стандартные селекторы могут не работать
        // Пробуем сначала стандартные селекторы
        String[] selectors = {
            ".price-block__final-price",
            ".price__main",
            "[data-product-price]",
            ".price",
            "[class*='price']",
            ".product-price",
            ".final-price",
            ".current-price"
        };
        
        double price = findPriceWithMultipleSelectors(doc, selectors);
        if (price > 0) {
            return price;
        }
        
        // Если стандартные методы не сработали, пробуем извлечь из JSON-LD
        return extractMvideoPriceFromJsonLd(doc);
    }
    
    // Извлечение цены М.Видео из JSON-LD
    private double extractMvideoPriceFromJsonLd(Document doc) {
        Elements scripts = doc.select("script[type='application/ld+json']");
        for (Element script : scripts) {
            String json = script.html();
            try {
                // Ищем цену в JSON-LD структурированных данных
                if (json.contains("\"price\"")) {
                    // Пробуем несколько паттернов для извлечения цены
                    Pattern[] pricePatterns = {
                        Pattern.compile("\"price\"\\s*:\\s*\"?([^\",}]+)\"?"),
                        Pattern.compile("\"offers\"\\s*:\\s*{[^}]*\"price\"\\s*:\\s*\"?([^\",}]+)\"?"),
                        Pattern.compile("\"currentPrice\"\\s*:\\s*\"?([^\",}]+)\"?")
                    };
                    
                    for (Pattern pattern : pricePatterns) {
                        Matcher matcher = pattern.matcher(json);
                        if (matcher.find()) {
                            try {
                                return parsePriceString(matcher.group(1));
                            } catch (Exception e) {
                                continue;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return 0.0;
    }

    // Эльдорадо
    private double extractEldoradoPrice(Document doc) {
        String[] selectors = {
            ".price__current",
            ".price-block__current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Технопарк
    private double extractTechnoparkPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Пульт.ру
    private double extractPultPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price__current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // ОнлайнТрейд.ру
    private double extractOnlinetradePrice(Document doc) {
        String[] selectors = {
            ".price_current",
            ".product-price__current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Холодильник.ру
    private double extractHolodilnikPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price__current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Перекрёсток
    private double extractPerekrestokPrice(Document doc) {
        String[] selectors = {
            ".price-regular",
            ".product-card-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Ашан
    private double extractAuchanPrice(Document doc) {
        String[] selectors = {
            ".product-price",
            ".price-current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Delivery Club
    private double extractDeliveryClubPrice(Document doc) {
        String[] selectors = {
            ".product-price",
            ".price-current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Самокат
    private double extractSamokatPrice(Document doc) {
        String[] selectors = {
            ".product-price",
            ".price-current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Ozon Travel
    private double extractOzonTravelPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".ticket-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Aviasales
    private double extractAviasalesPrice(Document doc) {
        String[] selectors = {
            ".ticket-price",
            ".price-current",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // СДЭК
    private double extractCdekPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".service-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Boxberry
    private double extractBoxberryPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".service-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Беру
    private double extractBeruPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Goods.ru
    private double extractGoodsPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Alltime
    private double extractAlltimePrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Sharmedia
    private double extractSharmediaPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Kant
    private double extractKantPrice(Document doc) {
        String[] selectors = {
            ".price-current",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Лабиринт
    private double extractLabirintPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Book24
    private double extractBook24Price(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Читай-город
    private double extractChitaiGorodPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Litres
    private double extractLitresPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // My-shop
    private double extractMyshopPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Oldi
    private double extractOldiPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Pleer
    private double extractPleerPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // DrHead
    private double extractDrheadPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Fotosklad
    private double extractFotoskladPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Prophotos
    private double extractProphotosPrice(Document doc) {
        String[] selectors = {
            ".price",
            ".product-price",
            "[data-price]",
            ".price",
            "[class*='price']"
        };
        return findPriceWithMultipleSelectors(doc, selectors);
    }

    // Вспомогательный метод для множественных селекторов
    private double findPriceWithMultipleSelectors(Document doc, String[] selectors) {
        for (String selector : selectors) {
            Elements elements = doc.select(selector);
            double price = findPriceInElements(elements);
            if (price > 0) {
                return price;
            }
        }
        return 0.0;
    }

    public boolean isUrlSupported(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        return url.startsWith("http://") || url.startsWith("https://");
    }
    
    /**
     * Маскирует URL для логирования (защита конфиденциальных данных)
     */
    private String maskUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        
        try {
            java.net.URI uri = new java.net.URI(url);
            String domain = uri.getHost();
            String path = uri.getPath();
            
            if (domain != null) {
                // Оставляем только домен первого уровня
                String[] parts = domain.split("\\.");
                if (parts.length >= 2) {
                    domain = parts[parts.length - 2] + "." + parts[parts.length - 1];
                }
            }
            
            return "https://" + domain + "/***";
        } catch (Exception e) {
            return url.substring(0, Math.min(url.length(), 30)) + "***";
        }
    }
}
