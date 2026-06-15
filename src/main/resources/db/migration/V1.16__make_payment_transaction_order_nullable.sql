-- Flyway Migration V1.16: Make order_id nullable in PaymentTransaction
ALTER TABLE PaymentTransaction ALTER COLUMN order_id INT NULL;
