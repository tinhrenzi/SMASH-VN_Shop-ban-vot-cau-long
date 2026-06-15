-- Flyway Migration V1.9: Make id_tai_khoan column in EditLog nullable
-- Database: SQL Server

ALTER TABLE EditLog ALTER COLUMN id_tai_khoan INT NULL;
