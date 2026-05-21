package grpc.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String projectRoot = System.getProperty("user.dir");
        String avatarPath = projectRoot.replace("\\", "/") + "/avatars/";
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + avatarPath);
    }
}
