-- Посмотреть всех пользователей в таблице users вашего приложения
SELECT id, email, first_name, last_name, public_id, created_at FROM users;

-- Посмотреть количество пользователей
SELECT COUNT(*) FROM users;

-- Посмотреть пользователей с аватарами
SELECT id, email, first_name, avatar_path FROM users WHERE avatar_path IS NOT NULL;
