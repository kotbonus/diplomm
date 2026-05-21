package grpc.demo.controller;

import grpc.demo.model.WishlistItem;
import grpc.demo.service.PriceParsingService;
import grpc.demo.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class WebController {

    @Autowired
    private WishlistService wishlistService;
    
    @Autowired
    private PriceParsingService priceParsingService;

    @GetMapping("/")
    public String home(Model model, HttpSession session) {
        model.addAttribute("title", "Мой Wishlist - Список желаний");
        model.addAttribute("message", "Добро пожаловать в ваш личный список желаний!");
        model.addAttribute("currentTime", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
        
        // Добавлние статистики для имеющихся пользователей
        if (session.getAttribute("currentUser") != null) {
            grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
            List<WishlistItem> userItems = wishlistService.getItemsByUser(currentUser.getId());
            model.addAttribute("totalItems", userItems.size());
            model.addAttribute("purchasedItems", userItems.stream().filter(WishlistItem::isPurchased).count());
        }
        
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("title", "О сайте");
        model.addAttribute("description", "Это приложение для управления вашим личным списком желаний, созданное с использованием Spring Boot и запущенное в Docker контейнере");
        return "about";
    }

    @GetMapping("/debug")
    public String debug(Model model, HttpSession session) {
        model.addAttribute("title", "Отладка сессии");
        return "debug";
    }

    
    @GetMapping("/wishlist")
    public String wishlist(@RequestParam(name = "category", required = false) String category,
                          Model model, HttpSession session, RedirectAttributes redirectAttributes) {

        System.out.println("Запрос к /wishlist");
        System.out.println("Сессия ID: " + session.getId());
        System.out.println("Пользователь в сессии: " + session.getAttribute("currentUser"));
        
        if (session.getAttribute("currentUser") == null) {
            System.out.println("Пользователь не авторизован, перенаправляем на вход");
            redirectAttributes.addFlashAttribute("error", "Для доступа к списку желаний необходимо войти в систему");
            return "redirect:/auth/login";
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        System.out.println("Пользователь авторизован, показываем вишлист для пользователя ID: " + currentUser.getId());
        model.addAttribute("title", "Мой Wishlist");
        
        List<WishlistItem> items;
        if (category != null && !category.isEmpty()) {
            items = wishlistService.getItemsByCategoryAndUser(category, currentUser.getId());
        } else {
            items = wishlistService.getItemsByUser(currentUser.getId());
        }
        
        model.addAttribute("items", items);
        model.addAttribute("categories", wishlistService.getAllCategoriesByUser(currentUser.getId()));
        model.addAttribute("selectedCategory", category);
        
        return "wishlist";
    }

    @GetMapping("/wishlist/add")
    public String showAddForm(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("currentUser") == null) {
            redirectAttributes.addFlashAttribute("error", "Для добавления желаний необходимо войти в систему");
            return "redirect:/auth/login";
        }
        
        model.addAttribute("title", "Добавить в Wishlist");
        model.addAttribute("item", new WishlistItem());
        model.addAttribute("categories", wishlistService.getAllCategories());
        return "add-item";
    }

    @PostMapping("/wishlist/add")
    public String addItem(@ModelAttribute WishlistItem item, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("currentUser") == null) {
            redirectAttributes.addFlashAttribute("error", "Для добавления желаний необходимо войти в систему");
            return "redirect:/auth/login";
        }

        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        item.setUserId(currentUser.getId());
        
        wishlistService.addItem(item);
        return "redirect:/wishlist";
    }

    @GetMapping("/wishlist/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("currentUser") == null) {
            redirectAttributes.addFlashAttribute("error", "Для редактирования желаний необходимо войти в систему");
            return "redirect:/auth/login";
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        Optional<WishlistItem> item = wishlistService.getItemById(id);
        
        if (item.isPresent() && item.get().getUserId().equals(currentUser.getId())) {
            model.addAttribute("title", "Редактировать элемент");
            model.addAttribute("item", item.get());
            model.addAttribute("categories", wishlistService.getAllCategoriesByUser(currentUser.getId()));
            return "edit-item";
        } else {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для редактирования этого элемента");
            return "redirect:/wishlist";
        }
    }

    @PostMapping("/wishlist/edit/{id}")
    public String editItem(@PathVariable Long id, @ModelAttribute WishlistItem item, Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("currentUser") == null) {
            redirectAttributes.addFlashAttribute("error", "Для редактирования желаний необходимо войти в систему");
            return "redirect:/auth/login";
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        Optional<WishlistItem> existingItem = wishlistService.getItemById(id);
        
        if (existingItem.isPresent() && existingItem.get().getUserId().equals(currentUser.getId())) {
            wishlistService.updateItem(id, item);
            return "redirect:/wishlist";
        } else {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для редактирования этого элемента");
            return "redirect:/wishlist";
        }
    }

    @GetMapping("/wishlist/delete/{id}")
    public String deleteItem(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("currentUser") == null) {
            redirectAttributes.addFlashAttribute("error", "Для удаления желаний необходимо войти в систему");
            return "redirect:/auth/login";
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        Optional<WishlistItem> item = wishlistService.getItemById(id);
        
        if (item.isPresent() && item.get().getUserId().equals(currentUser.getId())) {
            wishlistService.deleteItem(id);
        } else {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для удаления этого элемента");
        }
        
        return "redirect:/wishlist";
    }

    @GetMapping("/wishlist/toggle/{id}")
    public String togglePurchased(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("currentUser") == null) {
            redirectAttributes.addFlashAttribute("error", "Для изменения статуса желаний необходимо войти в систему");
            return "redirect:/auth/login";
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        Optional<WishlistItem> item = wishlistService.getItemById(id);
        
        if (item.isPresent() && item.get().getUserId().equals(currentUser.getId())) {
            wishlistService.togglePurchased(id);
        } else {
            redirectAttributes.addFlashAttribute("error", "У вас нет прав для изменения этого элемента");
        }
        
        return "redirect:/wishlist";
    }
    
    @PostMapping("/api/parse-price")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> parsePrice(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        Map<String, Object> response = new HashMap<>();
        
        if (url == null || url.trim().isEmpty()) {
            response.put("success", false);
            response.put("error", "URL не указан");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (!priceParsingService.isUrlSupported(url)) {
            response.put("success", false);
            response.put("error", "Некорректный URL");
            return ResponseEntity.badRequest().body(response);
        }
        
        PriceParsingService.PriceResult result = priceParsingService.parsePriceFromUrl(url);
        
        response.put("success", result.isSuccess());
        response.put("price", result.getPrice());
        response.put("oldPrice", result.getOldPrice());
        response.put("discount", result.getDiscount());
        response.put("error", result.getError());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/api/update-prices")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updatePrices(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (session.getAttribute("currentUser") == null) {
            response.put("success", false);
            response.put("error", "Для обновления цен необходимо войти в систему");
            return ResponseEntity.badRequest().body(response);
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        int updatedCount = 0;
        int discountCount = 0;
        
        for (WishlistItem item : wishlistService.getItemsByUser(currentUser.getId())) {
            if (item.isAutoPriceTracking() && item.getLink() != null && !item.getLink().trim().isEmpty()) {
                PriceParsingService.PriceResult result = priceParsingService.parsePriceFromUrl(item.getLink());
                
                if (result.isSuccess()) {
                    double oldPrice = item.getPrice();
                    double newPrice = result.getPrice();
                    
                    if (newPrice != oldPrice) {
                        item.setPrice(newPrice);
                        // Если есть информация о старой цене с сайта, обновляем её
                        if (result.getOldPrice() > 0) {
                            item.setOriginalPrice(result.getOldPrice());
                        }
                        wishlistService.updateItem(item.getId(), item);
                        updatedCount++;
                        
                        // Проверяем наличие скидки (явная или рассчитанная)
                        if (result.getDiscount() > 0 || (result.getOldPrice() > 0 && newPrice < result.getOldPrice())) {
                            discountCount++;
                        }
                    }
                }
            }
        }
        
        response.put("success", true);
        response.put("updatedCount", updatedCount);
        response.put("discountCount", discountCount);
        response.put("message", String.format("Обновлено %d цен, из них со скидкой: %d", updatedCount, discountCount));
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/api/reserve-item")
    public ResponseEntity<Map<String, Object>> reserveItem(@RequestParam Long itemId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (session.getAttribute("currentUser") == null) {
            response.put("success", false);
            response.put("message", "Необходимо войти в систему");
            return ResponseEntity.status(401).body(response);
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        boolean success = wishlistService.reserveItem(itemId, currentUser.getId());
        
        if (success) {
            response.put("success", true);
            response.put("message", "Желание успешно забронировано");
        } else {
            response.put("success", false);
            response.put("message", "Не удалось забронировать желание. Возможно, оно уже забронировано или куплено.");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/api/cancel-reservation")
    public ResponseEntity<Map<String, Object>> cancelReservation(@RequestParam Long itemId, HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        
        if (session.getAttribute("currentUser") == null) {
            response.put("success", false);
            response.put("message", "Необходимо войти в систему");
            return ResponseEntity.status(401).body(response);
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        boolean success = wishlistService.cancelReservation(itemId, currentUser.getId());
        
        if (success) {
            response.put("success", true);
            response.put("message", "Бронь успешно отменена");
        } else {
            response.put("success", false);
            response.put("message", "Не удалось отменить бронь. Вы можете отменить только свою бронь или бронь на своих желаниях.");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api/reserved-items")
    public ResponseEntity<List<WishlistItem>> getReservedItems(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            return ResponseEntity.status(401).build();
        }
        
        grpc.demo.model.User currentUser = (grpc.demo.model.User) session.getAttribute("currentUser");
        List<WishlistItem> reservedItems = wishlistService.getReservedItemsByUser(currentUser.getId());
        
        return ResponseEntity.ok(reservedItems);
    }
}
