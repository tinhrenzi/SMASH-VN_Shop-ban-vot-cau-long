-- Flyway Migration V1.11: Fix role flags for existing accounts
-- Database: SQL Server

UPDATE TaiKhoan
SET la_khach_hang = 1
WHERE vai_tro = 'KH' AND (la_khach_hang = 0 OR la_khach_hang IS NULL);

UPDATE TaiKhoan
SET la_nhan_vien = 1
WHERE vai_tro = 'NV' AND (la_nhan_vien = 0 OR la_nhan_vien IS NULL);

UPDATE TaiKhoan
SET la_quan_ly = 1
WHERE vai_tro = 'QL' AND (la_quan_ly = 0 OR la_quan_ly IS NULL);
