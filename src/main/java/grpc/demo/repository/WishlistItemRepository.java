package grpc.demo.repository;

import grpc.demo.model.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    
    List<WishlistItem> findByUserId(Long userId);
    
    List<WishlistItem> findByUserIdAndCategory(Long userId, String category);
    
    List<WishlistItem> findByUserIdAndPurchased(Long userId, boolean purchased);
    
    @Query("SELECT w FROM WishlistItem w WHERE w.userId = :userId AND w.autoPriceTracking = true AND w.link IS NOT NULL AND w.link != ''")
    List<WishlistItem> findItemsWithAutoTrackingByUserId(@Param("userId") Long userId);
    
    @Query("SELECT w FROM WishlistItem w WHERE w.userId = :userId AND w.discountAvailable = true")
    List<WishlistItem> findItemsWithDiscountsByUserId(@Param("userId") Long userId);
}
