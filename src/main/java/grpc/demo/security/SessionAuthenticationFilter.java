package grpc.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SessionAuthenticationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Проверяем наличие пользователя в сессии (для веб-страниц)
        Object user = request.getSession().getAttribute("currentUser");
        
        if (user != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Если пользователь в сессии, но нет аутентификации в контексте Spring Security
            try {
                // Создаем простую аутентификацию на основе данных из сессии
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(
                        user, null, java.util.Collections.emptyList());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                logger.debug("Пользователь аутентифицирован через сессию");

                MDC.put("authenticated", "true");
                
            } catch (Exception e) {
                logger.warn("Ошибка при аутентификации через сессию: {}", e.getMessage());
            }
        }
        
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();

        return path.startsWith("/api/") || 
               path.startsWith("/auth/") || 
               path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.startsWith("/h2-console") ||
               path.equals("/") ||
               path.startsWith("/actuator/");
    }
}
