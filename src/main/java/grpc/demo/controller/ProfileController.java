package grpc.demo.controller;

import grpc.demo.model.User;
import grpc.demo.model.WishlistItem;
import grpc.demo.service.UserService;
import grpc.demo.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final String AVATAR_DIRECTORY = "avatars";
    
    @Autowired
    private UserService userService;

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public String profile(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("title", "Личный кабинет");
        model.addAttribute("user", currentUser);
        
        // Статистика пользователя
        List<WishlistItem> userItems = wishlistService.getItemsByUser(currentUser.getId());
        model.addAttribute("totalItems", userItems.size());
        model.addAttribute("purchasedItems", userItems.stream()
                .filter(WishlistItem::isPurchased).count());
        model.addAttribute("pendingItems", userItems.stream()
                .filter(item -> !item.isPurchased()).count());

        return "profile";
    }

    @GetMapping("/edit")
    public String showEditProfileForm(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("title", "Редактировать профиль");
        model.addAttribute("user", currentUser);
        return "edit-profile";
    }

    @PostMapping("/edit")
    public String editProfile(@RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam(value = "about", required = false) String about,
                             @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                             HttpSession session,
                             Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        currentUser.setFirstName(firstName);
        currentUser.setLastName(lastName);
        currentUser.setAbout(about);
        
        // Обработка загрузки аватара
        if (avatar != null && !avatar.isEmpty()) {
            try {
                // Создаем директорию для аватаров, если она не существует
                Path uploadPath = Paths.get(AVATAR_DIRECTORY);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }
                
                // Генерируем уникальное имя файла
                String originalFilename = StringUtils.cleanPath(avatar.getOriginalFilename());
                String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
                String newFilename = "user_" + currentUser.getId() + "_" + System.currentTimeMillis() + fileExtension;
                
                // Сохраняем файл
                Path filePath = uploadPath.resolve(newFilename);
                Files.copy(avatar.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                
                // Обновляем путь к аватару в пользователе
                currentUser.setAvatarPath("/" + AVATAR_DIRECTORY + "/" + newFilename);
                
            } catch (IOException ex) {
                model.addAttribute("error", "Ошибка при загрузке аватара: " + ex.getMessage());
                model.addAttribute("title", "Редактировать профиль");
                model.addAttribute("user", currentUser);
                return "edit-profile";
            }
        }
        
        // Сохраняем изменения в базе данных
        userService.updateUser(currentUser);
        
        session.setAttribute("currentUser", currentUser);
        
        model.addAttribute("title", "Личный кабинет");
        model.addAttribute("user", currentUser);
        model.addAttribute("success", "Профиль успешно обновлен!");
        
        return "profile";
    }
}
