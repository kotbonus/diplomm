package grpc.demo.service;

import grpc.demo.model.WishlistItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WishlistService {
    private List<WishlistItem> wishlistItems = new ArrayList<>();
    private Long nextId = 1L;

    public WishlistService() {
        initializeSampleData();
    }

    private void initializeSampleData() {
        // Test data removed - wishlist starts empty
    }

    public List<WishlistItem> getAllItems() {
        return new ArrayList<>(wishlistItems);
    }
    
    public List<WishlistItem> getItemsByUser(Long userId) {
        return wishlistItems.stream()
                .filter(item -> item.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    public List<WishlistItem> getItemsByCategoryAndUser(String category, Long userId) {
        return wishlistItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .filter(item -> item.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    public List<WishlistItem> getItemsByCategory(String category) {
        return wishlistItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public Optional<WishlistItem> getItemById(Long id) {
        return wishlistItems.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
    }

    public WishlistItem addItem(WishlistItem item) {
        item.setId(nextId++);
        item.setCreatedAt(java.time.LocalDateTime.now());
        wishlistItems.add(item);
        return item;
    }

    public boolean updateItem(Long id, WishlistItem updatedItem) {
        Optional<WishlistItem> existingItem = getItemById(id);
        if (existingItem.isPresent()) {
            WishlistItem item = existingItem.get();
            item.setTitle(updatedItem.getTitle());
            item.setDescription(updatedItem.getDescription());
            item.setLink(updatedItem.getLink());
            
            // Обновляем цену и связанные поля
            double newPrice = updatedItem.getPrice();
            if (item.getPrice() != newPrice) {
                item.setPrice(newPrice);
            } else {
                item.setPrice(newPrice);
            }
            
            item.setCategory(updatedItem.getCategory());
            item.setPriority(updatedItem.getPriority());
            item.setAutoPriceTracking(updatedItem.isAutoPriceTracking());
            return true;
        }
        return false;
    }

    public boolean deleteItem(Long id) {
        return wishlistItems.removeIf(item -> item.getId().equals(id));
    }

    public boolean togglePurchased(Long id) {
        Optional<WishlistItem> item = getItemById(id);
        if (item.isPresent()) {
            item.get().setPurchased(!item.get().isPurchased());
            return true;
        }
        return false;
    }

    public List<String> getAllCategories() {
        return wishlistItems.stream()
                .map(WishlistItem::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<String> getAllCategoriesByUser(Long userId) {
        return wishlistItems.stream()
                .filter(item -> item.getUserId().equals(userId))
                .map(WishlistItem::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<WishlistItem> getItemsWithAutoTracking() {
        return wishlistItems.stream()
                .filter(WishlistItem::isAutoPriceTracking)
                .filter(item -> item.getLink() != null && !item.getLink().trim().isEmpty())
                .collect(Collectors.toList());
    }
    
    public List<WishlistItem> getItemsWithDiscounts() {
        return wishlistItems.stream()
                .filter(WishlistItem::isDiscountAvailable)
                .collect(Collectors.toList());
    }
    
    public boolean reserveItem(Long itemId, Long userId) {
        Optional<WishlistItem> item = getItemById(itemId);
        if (item.isPresent() && !item.get().isReserved() && !item.get().isPurchased()) {
            WishlistItem wishlistItem = item.get();
            wishlistItem.setReserved(true);
            wishlistItem.setReservedBy(userId);
            wishlistItem.setReservedAt(java.time.LocalDateTime.now());
            return true;
        }
        return false;
    }
    
    public boolean cancelReservation(Long itemId, Long userId) {
        Optional<WishlistItem> item = getItemById(itemId);
        if (item.isPresent() && item.get().isReserved() && 
            (item.get().getReservedBy().equals(userId) || item.get().getUserId().equals(userId))) {
            WishlistItem wishlistItem = item.get();
            wishlistItem.setReserved(false);
            wishlistItem.setReservedBy(null);
            wishlistItem.setReservedAt(null);
            return true;
        }
        return false;
    }
    
    public List<WishlistItem> getReservedItemsByUser(Long userId) {
        return wishlistItems.stream()
                .filter(item -> item.getReservedBy() != null && item.getReservedBy().equals(userId))
                .collect(Collectors.toList());
    }
}
