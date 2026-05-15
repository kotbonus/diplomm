package grpc.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class WishlistItemRequest {
    
    @NotBlank(message = "Название не может быть пустым")
    @Size(min = 1, max = 200, message = "Название должно содержать от 1 до 200 символов")
    private String title;
    
    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    private String description;
    
    @Pattern(regexp = "^$|^https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?$", 
             message = "Некорректный формат URL")
    private String link;
    
    @Pattern(regexp = "^$|^\\d+(\\.\\d{1,2})?$", 
             message = "Цена должна быть числом с не более чем 2 знаками после запятой")
    private String price;
    
    @Size(max = 50, message = "Категория не должна превышать 50 символов")
    private String category;
    
    @Size(max = 50, message = "Приоритет не должен превышать 50 символов")
    private String priority;

    public WishlistItemRequest() {}

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

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
