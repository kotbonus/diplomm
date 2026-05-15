-- Добавляем поле about если его нет
ALTER TABLE users ADD COLUMN IF NOT EXISTS about TEXT;

-- Обновляем тестовых пользователей с описанием о себе
UPDATE users SET about = 'Люблю читать книги и путешествовать. Всегда рад новым знакомствам!' WHERE email = 'admin@wishlist.com';
UPDATE users SET about = 'Технологический энтузиаст, программист и геймер. В свободное время занимаюсь фотографией.' WHERE email = 'ivan@example.com';
UPDATE users SET about = 'Ценю уют и комфорт. Люблю готовить и создавать уют в доме. Мечтаю о кругосветном путешествии.' WHERE email = 'maria@example.com';
UPDATE users SET about = 'Спортивный и активный образ жизни. Люблю экстремальные виды спорта и новые вызовы.' WHERE email = 'test@example.com';
