-- Очистка всех таблиц
DROP TABLE IF EXISTS wishlist_items CASCADE;
DROP TABLE IF EXISTS friendships CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Пересоздание таблиц (Hibernate сделает это автоматически)
