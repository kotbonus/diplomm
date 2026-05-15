package grpc.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrationRequest {
    
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Size(max = 100, message = "Email не должен превышать 100 символов")
    private String email;
    
    @NotBlank(message = "Пароль не может быть пустым")
    @Size(min = 6, max = 50, message = "Пароль должен содержать от 6 до 50 символов")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", 
             message = "Пароль должен содержать хотя бы одну букву и одну цифру")
    private String password;
    
    @NotBlank(message = "Имя не может быть пустым")
    @Size(min = 2, max = 50, message = "Имя должно содержать от 2 до 50 символов")
    @Pattern(regexp = "^[A-Za-zА-Яа-я\\s\\-']+$", 
             message = "Имя может содержать только буквы, пробелы, дефисы и апострофы")
    private String firstName;
    
    @NotBlank(message = "Фамилия не может быть пустой")
    @Size(min = 2, max = 50, message = "Фамилия должна содержать от 2 до 50 символов")
    @Pattern(regexp = "^[A-Za-zА-Яа-я\\s\\-']+$", 
             message = "Фамилия может содержать только буквы, пробелы, дефисы и апострофы")
    private String lastName;

    public RegistrationRequest() {}

    public RegistrationRequest(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
