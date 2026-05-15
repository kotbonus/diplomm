-- Посмотреть всех пользователей в базе данных
\du

-- Или через SQL запрос
SELECT usename AS "Имя пользователя", 
       usecreatedb AS "Создает БД", 
       usesuper AS "Суперпользователь",
       usecreaterole AS "Создает роли"
FROM pg_user;

-- Посмотреть текущего пользователя
SELECT current_user;

-- Посмотреть все базы данных
\l

-- Подключиться к вашей базе данных
\c wishlistdb
