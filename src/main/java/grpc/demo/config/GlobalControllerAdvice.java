package grpc.demo.config;

import grpc.demo.model.User;
import grpc.demo.service.FriendshipService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {
    
    @Autowired
    private FriendshipService friendshipService;
    
    @ModelAttribute
    public void addGlobalAttributes(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        
        if (currentUser != null) {
            // Добавляем счетчик входящих заявок в друзья
            long pendingRequestsCount = friendshipService.countPendingRequests(currentUser.getId());
            model.addAttribute("pendingRequestsCount", pendingRequestsCount);
        } else {
            model.addAttribute("pendingRequestsCount", 0);
        }
    }
}
