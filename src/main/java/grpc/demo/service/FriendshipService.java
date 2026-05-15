package grpc.demo.service;

import grpc.demo.model.Friendship;
import grpc.demo.model.User;
import grpc.demo.repository.FriendshipRepository;
import grpc.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService {
    
    @Autowired
    private FriendshipRepository friendshipRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findAcceptedFriendships(userId);
        List<Long> friendIds = friendships.stream()
                .map(f -> f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId())
                .toList();
        
        return userRepository.findAllById(friendIds);
    }
    
    public List<Friendship> getPendingRequests(Long userId) {
        return friendshipRepository.findPendingRequests(userId);
    }
    
    public Friendship sendFriendRequest(Long userId, Long friendId) {
        if (friendshipRepository.existsByUserIdAndFriendId(userId, friendId) ||
            friendshipRepository.existsByUserIdAndFriendId(friendId, userId)) {
            throw new IllegalArgumentException("Запрос на дружбу уже существует или вы уже друзья");
        }
        
        Friendship friendship = new Friendship(userId, friendId);
        friendship.setStatus(Friendship.FriendshipStatus.PENDING);
        return friendshipRepository.save(friendship);
    }
    
    public Friendship acceptFriendRequest(Long friendshipId, Long userId) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findById(friendshipId);
        if (friendshipOpt.isEmpty()) {
            throw new IllegalArgumentException("Запрос на дружбу не найден");
        }
        
        Friendship friendship = friendshipOpt.get();
        if (!friendship.getFriendId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете принять этот запрос");
        }
        
        friendship.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        return friendshipRepository.save(friendship);
    }
    
    public void rejectFriendRequest(Long friendshipId, Long userId) {
        Optional<Friendship> friendshipOpt = friendshipRepository.findById(friendshipId);
        if (friendshipOpt.isEmpty()) {
            throw new IllegalArgumentException("Запрос на дружбу не найден");
        }
        
        Friendship friendship = friendshipOpt.get();
        if (!friendship.getFriendId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете отклонить этот запрос");
        }
        
        friendshipRepository.delete(friendship);
    }
    
    public void removeFriend(Long userId, Long friendId) {
        Optional<Friendship> friendship1 = friendshipRepository.findByUsers(userId, friendId);
        Optional<Friendship> friendship2 = friendshipRepository.findByUsers(friendId, userId);
        
        friendship1.ifPresent(friendshipRepository::delete);
        friendship2.ifPresent(friendshipRepository::delete);
    }
    
    public List<User> searchUsers(String search, Long currentUserId) {
        return userRepository.searchByPublicIdOrEmail(search).stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .toList();
    }
    
    public Optional<User> findByPublicId(String publicId) {
        return userRepository.findByPublicId(publicId);
    }
    
    public long countPendingRequests(Long userId) {
        return friendshipRepository.countPendingRequests(userId);
    }
}
