package grpc.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "wishlist_items")
public class WishlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String title;
    
    private String description;
    private String link;
    private double price;
    private double originalPrice;
    private boolean autoPriceTracking;
    
    @Column(name = "last_price_update")
    private LocalDateTime lastPriceUpdate;
    
    @Column(name = "discount_available")
    private boolean discountAvailable;
    
    private String category;
    
    private int priority;
    private String imageUrl;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    private boolean purchased;
    
    @Column(name = "is_reserved")
    private boolean isReserved;
    
    @Column(name = "reserved_by")
    private Long reservedBy;
    
    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    public WishlistItem() {
        this.createdAt = LocalDateTime.now();
        this.purchased = false;
        this.isReserved = false;
        this.priority = 1;
        this.autoPriceTracking = false;
        this.originalPrice = 0.0;
        this.lastPriceUpdate = null;
        this.discountAvailable = false;
    }

    public WishlistItem(String title, String description, String link, double price, String category, int priority) {
        this();
        this.title = title;
        this.description = description;
        this.link = link;
        this.price = price;
        this.originalPrice = price;
        this.category = category;
        this.priority = priority;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
        if (this.originalPrice == 0.0) {
            this.originalPrice = price;
        }
        this.lastPriceUpdate = LocalDateTime.now();
        this.discountAvailable = price < this.originalPrice;
    }

    public double getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(double originalPrice) {
        this.originalPrice = originalPrice;
    }

    public boolean isAutoPriceTracking() {
        return autoPriceTracking;
    }

    public void setAutoPriceTracking(boolean autoPriceTracking) {
        this.autoPriceTracking = autoPriceTracking;
    }

    public LocalDateTime getLastPriceUpdate() {
        return lastPriceUpdate;
    }

    public void setLastPriceUpdate(LocalDateTime lastPriceUpdate) {
        this.lastPriceUpdate = lastPriceUpdate;
    }

    public boolean isDiscountAvailable() {
        return discountAvailable;
    }

    public void setDiscountAvailable(boolean discountAvailable) {
        this.discountAvailable = discountAvailable;
    }

    public double getDiscountPercentage() {
        if (originalPrice > 0 && price < originalPrice) {
            return Math.round((1 - price / originalPrice) * 100);
        }
        return 0;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public boolean isReserved() {
        return isReserved;
    }

    public void setReserved(boolean reserved) {
        isReserved = reserved;
    }

    public Long getReservedBy() {
        return reservedBy;
    }

    public void setReservedBy(Long reservedBy) {
        this.reservedBy = reservedBy;
    }

    public LocalDateTime getReservedAt() {
        return reservedAt;
    }

    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }
}
