-- Flyway Migration V1.14: Update Giao Hàng Nhanh (GHN) API credentials and warehouse address
-- Database: SQL Server

UPDATE DonViVanChuyen
SET token = '7cb9910e-6313-11f1-a973-aee5264794df',
    client_id = '2511718',
    dia_chi_kho = N'Nơi test thử sản phẩm lập trình, Xã Quyết Thắng, Thành phố Thái Nguyên, Thái Nguyên'
WHERE ten_don_vi LIKE N'%Giao Hàng Nhanh%' OR ten_don_vi LIKE N'%GHN%';
