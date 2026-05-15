// JWT Authentication utilities
class JwtAuth {
    constructor() {
        this.token = localStorage.getItem('jwtToken');
        this.user = JSON.parse(localStorage.getItem('currentUser') || 'null');
    }

    // Сохранение токена и данных пользователя
    setToken(token, user) {
        this.token = token;
        this.user = user;
        localStorage.setItem('jwtToken', token);
        localStorage.setItem('currentUser', JSON.stringify(user));
    }

    // Получение токена
    getToken() {
        return this.token;
    }

    // Получение данных пользователя
    getUser() {
        return this.user;
    }

    // Проверка авторизации
    isAuthenticated() {
        return !!this.token;
    }

    // Выход
    logout() {
        this.token = null;
        this.user = null;
        localStorage.removeItem('jwtToken');
        localStorage.removeItem('currentUser');
        window.location.href = '/auth/login';
    }

    // Добавление токена к заголовкам
    addAuthHeader(headers = {}) {
        if (this.token) {
            headers['Authorization'] = `Bearer ${this.token}`;
        }
        return headers;
    }

    // API запрос с аутентификацией
    async authenticatedFetch(url, options = {}) {
        const headers = this.addAuthHeader(options.headers || {});
        
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...headers
            }
        });

        if (response.status === 401) {
            // Токен истек или невалидный
            this.logout();
            return null;
        }

        return response;
    }

    // Логин через API
    async login(email, password) {
        try {
            const response = await fetch('/auth/api/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            if (response.ok) {
                const authResponse = await response.json();
                this.setToken(authResponse.token, authResponse);
                return { success: true, user: authResponse };
            } else {
                const error = await response.text();
                return { success: false, error };
            }
        } catch (error) {
            return { success: false, error: error.message };
        }
    }

    // Логаут через API
    async logoutApi() {
        try {
            await this.authenticatedFetch('/auth/api/logout', {
                method: 'POST'
            });
        } catch (error) {
            console.error('Ошибка при логауте:', error);
        } finally {
            this.logout();
        }
    }
}

// Глобальный экземпляр
window.jwtAuth = new JwtAuth();

// Функция для проверки авторизации при загрузке страницы
function checkAuth() {
    if (!window.jwtAuth.isAuthenticated()) {
        // Если не на странице логина или регистрации, перенаправляем
        const currentPath = window.location.pathname;
        if (!currentPath.startsWith('/auth/') && currentPath !== '/') {
            window.location.href = '/auth/login';
        }
    }
}

// Автоматическая проверка авторизации
document.addEventListener('DOMContentLoaded', function() {
    // Проверяем авторизацию только для защищенных страниц
    const currentPath = window.location.pathname;
    const protectedPaths = ['/wishlist', '/profile', '/friends', '/add-item'];
    
    if (protectedPaths.some(path => currentPath.startsWith(path))) {
        checkAuth();
    }
});

// Обработка форм логина
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.querySelector('#loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const email = document.querySelector('#email').value;
            const password = document.querySelector('#password').value;
            const submitBtn = document.querySelector('#submitBtn');
            const errorDiv = document.querySelector('#error');
            
            submitBtn.disabled = true;
            submitBtn.textContent = 'Вход...';
            errorDiv.style.display = 'none';
            
            try {
                const result = await window.jwtAuth.login(email, password);
                
                if (result.success) {
                    window.location.href = '/wishlist';
                } else {
                    errorDiv.textContent = result.error || 'Ошибка входа';
                    errorDiv.style.display = 'block';
                }
            } catch (error) {
                errorDiv.textContent = 'Произошла ошибка. Попробуйте еще раз.';
                errorDiv.style.display = 'block';
            } finally {
                submitBtn.disabled = false;
                submitBtn.textContent = 'Войти';
            }
        });
    }
    
    // Обработка кнопки логаута
    const logoutBtn = document.querySelector('#logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', function(e) {
            e.preventDefault();
            window.jwtAuth.logoutApi();
        });
    }
});
