-- Вставка тестовых пользователей с аватарками и описаниями
INSERT INTO users (email, password, first_name, last_name, avatar_path, public_id, about, created_at, last_login_at) VALUES
('admin@wishlist.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Admin', 'User', '/avatars/user_2_1772036903825.png', 'admin123', 'Администратор системы. Люблю технологии и помогать людям находить подарки для своих близких.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ivan@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Ivan', 'Petrov', '/avatars/user_7_1773751938963.jpeg', 'ivan123', 'Спортивный парень, увлекаюсь бегом и технологиями. Мечтаю пробежать марафон и собрать лучшую коллекцию гаджетов.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('maria@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Maria', 'Ivanova', '/avatars/user_8_1773751963437.jpeg', 'maria123', 'Творческая натура, обожаю читать книги и путешествовать. Всегда рада новым знакомствам и интересным историям.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('test@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'Test', 'User', NULL, 'test123', 'Тестовый пользователь для проверки функциональности системы.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('k2528870@gmail.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDa', 'User', 'K2528870', NULL, 'k2528870', 'Разработчик и создатель этого проекта. Верю в силу технологий для улучшения жизни людей.', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Вставка тестовых wishlist товаров с изображениями
INSERT INTO wishlist_items (user_id, title, description, link, price, original_price, auto_price_tracking, discount_available, category, priority, image_url, created_at, purchased) VALUES
(2, 'Кроссовки Nike Air Max', 'Отличные кроссовки для бега', 'https://example.com/nike-air-max', 8990.00, 12990.00, true, true, 'Спорт', 1, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=300', CURRENT_TIMESTAMP, false),
(2, 'iPhone 15 Pro', 'Новый флагман от Apple', 'https://example.com/iphone-15', 99990.00, 119990.00, true, true, 'Электроника', 2, 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=300', CURRENT_TIMESTAMP, false),
(3, 'Книга "Искусство войны"', 'Классическая стратегическая книга', 'https://example.com/art-of-war', 490.00, 690.00, false, true, 'Книги', 1, 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=300', CURRENT_TIMESTAMP, false),
(3, 'Футболка с принтом', 'Стильная хлопковая футболка', 'https://example.com/tshirt', 1290.00, 1890.00, false, true, 'Одежда', 2, 'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=300', CURRENT_TIMESTAMP, false);
