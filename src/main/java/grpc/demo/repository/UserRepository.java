package grpc.demo.repository;

import grpc.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.id = :id OR u.email LIKE %:search% OR u.firstName LIKE %:search% OR u.lastName LIKE %:search% OR CONCAT(u.firstName, ' ', u.lastName) LIKE %:search%")
    List<User> searchUsers(@Param("search") String search, @Param("id") Long id);
    
    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND u.id NOT IN " +
           "(SELECT f.friendId FROM Friendship f WHERE f.userId = :currentUserId AND f.status = 'ACCEPTED') " +
           "AND u.id NOT IN " +
           "(SELECT f.userId FROM Friendship f WHERE f.friendId = :currentUserId AND f.status = 'ACCEPTED')")
    List<User> findNonFriends(@Param("currentUserId") Long currentUserId);
    
    Optional<User> findByPublicId(String publicId);
    
    @Query("SELECT u FROM User u WHERE u.publicId LIKE %:search% OR u.email LIKE %:search% OR u.firstName LIKE %:search% OR u.lastName LIKE %:search% OR CONCAT(u.firstName, ' ', u.lastName) LIKE %:search%")
    List<User> searchByPublicIdOrEmail(@Param("search") String search);
    
    Optional<User> findByResetToken(String resetToken);
}
