package grpc.demo.repository;

import grpc.demo.model.Friendship;
import grpc.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    
    List<Friendship> findByUserIdAndStatus(Long userId, Friendship.FriendshipStatus status);
    
    List<Friendship> findByFriendIdAndStatus(Long friendId, Friendship.FriendshipStatus status);
    
    @Query("SELECT f FROM Friendship f WHERE (f.userId = :userId OR f.friendId = :userId) AND f.status = 'ACCEPTED'")
    List<Friendship> findAcceptedFriendships(@Param("userId") Long userId);
    
    @Query("SELECT f FROM Friendship f WHERE f.userId = :userId AND f.friendId = :friendId")
    Optional<Friendship> findByUsers(@Param("userId") Long userId, @Param("friendId") Long friendId);
    
    @Query("SELECT f FROM Friendship f WHERE f.friendId = :userId AND f.status = 'PENDING'")
    List<Friendship> findPendingRequests(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.friendId = :userId AND f.status = 'PENDING'")
    long countPendingRequests(@Param("userId") Long userId);
    
    boolean existsByUserIdAndFriendId(Long userId, Long friendId);
}
