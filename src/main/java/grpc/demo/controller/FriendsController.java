package grpc.demo.controller;

import grpc.demo.model.User;
import grpc.demo.model.WishlistItem;
import grpc.demo.repository.UserRepository;
import grpc.demo.service.FriendshipService;
import grpc.demo.service.WishlistService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/friends")
public class FriendsController {

    @Autowired
    private FriendshipService friendshipService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public String friendsList(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Для просмотра друзей необходимо войти в систему");
            return "redirect:/auth/login";
        }

        model.addAttribute("title", "Мои друзья");
        model.addAttribute("friends", friendshipService.getFriends(currentUser.getId()));
        model.addAttribute("pendingRequests", friendshipService.getPendingRequests(currentUser.getId()));
        
        return "friends";
    }

    @GetMapping("/search")
    public String searchFriends(@RequestParam(required = false) String search, 
                              Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Для поиска друзей необходимо войти в систему");
            return "redirect:/auth/login";
        }

        model.addAttribute("title", "Поиск друзей");
        
        if (search != null && !search.trim().isEmpty()) {
            List<User> foundUsers = friendshipService.searchUsers(search.trim(), currentUser.getId());
            model.addAttribute("search", search);
            model.addAttribute("foundUsers", foundUsers);
        }
        
        return "friends-search";
    }

    @PostMapping("/add")
    public String addFriend(@RequestParam String identifier, 
                           HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Для добавления друзей необходимо войти в систему");
            return "redirect:/auth/login";
        }

        try {
            Optional<User> friendOpt = friendshipService.findByPublicId(identifier);
            if (friendOpt.isEmpty()) {
                // Попробуем найти по email
                friendOpt = userRepository.findByEmail(identifier);
            }
            
            if (friendOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
                return "redirect:/friends/search";
            }
            
            User friend = friendOpt.get();
            if (friend.getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("error", "Нельзя добавить себя в друзья");
                return "redirect:/friends/search";
            }
            
            friendshipService.sendFriendRequest(currentUser.getId(), friend.getId());
            redirectAttributes.addFlashAttribute("success", "Запрос на дружбу отправлен!");
            
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/friends/search";
    }

    @PostMapping("/accept/{friendshipId}")
    public String acceptFriendRequest(@PathVariable Long friendshipId,
                                    HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            friendshipService.acceptFriendRequest(friendshipId, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Запрос на дружбу принят!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/friends";
    }

    @PostMapping("/reject/{friendshipId}")
    public String rejectFriendRequest(@PathVariable Long friendshipId,
                                    HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        try {
            friendshipService.rejectFriendRequest(friendshipId, currentUser.getId());
            redirectAttributes.addFlashAttribute("success", "Запрос на дружбу отклонен");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/friends";
    }

    @PostMapping("/remove/{friendId}")
    public String removeFriend(@PathVariable Long friendId,
                             HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login";
        }

        friendshipService.removeFriend(currentUser.getId(), friendId);
        redirectAttributes.addFlashAttribute("success", "Друг удален");
        
        return "redirect:/friends";
    }

    @GetMapping("/profile/{publicId}")
    public String viewFriendProfile(@PathVariable String publicId,
                                   Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            redirectAttributes.addFlashAttribute("error", "Для просмотра профиля необходимо войти в систему");
            return "redirect:/auth/login";
        }

        Optional<User> friendOpt = friendshipService.findByPublicId(publicId);
        if (friendOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/friends/search";
        }
        
        User friend = friendOpt.get();
        
        // Проверяем, является ли пользователь другом
        boolean isFriend = friendshipService.getFriends(currentUser.getId()).stream()
                .anyMatch(f -> f.getId().equals(friend.getId()));
        
        model.addAttribute("title", "Профиль " + friend.getFullName());
        model.addAttribute("friend", friend);
        model.addAttribute("isFriend", isFriend);
        
        // Показываем вишлист друга, если он друг
        if (isFriend) {
            List<WishlistItem> friendItems = wishlistService.getItemsByUser(friend.getId());
            model.addAttribute("friendItems", friendItems);
        }
        
        return "friend-profile";
    }
}
