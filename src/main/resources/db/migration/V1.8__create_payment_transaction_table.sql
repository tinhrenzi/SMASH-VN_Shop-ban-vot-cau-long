-- Flyway Migration V1.8: Add ma_don_hang to HoaDon and Create PaymentTransaction table for SePay IPN
-- Database: SQL Server

-- 1. Add ma_don_hang to HoaDon if not exists
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ma_don_hang')
BEGIN
    ALTER TABLE HoaDon ADD ma_don_hang VARCHAR(50) NULL;
END;

-- 2. Populate legacy orders using dynamic SQL to bypass SQL Server's compile-time column validation
EXEC('
UPDATE HoaDon 
SET ma_don_hang = ''LEGACY_'' + CAST(id AS VARCHAR(20))
WHERE ma_don_hang IS NULL;
');

-- 3. Resolve any duplicate ma_don_hang values that might exist
EXEC('
IF EXISTS (
    SELECT ma_don_hang, COUNT(*) 
    FROM HoaDon 
    WHERE ma_don_hang IS NOT NULL 
    GROUP BY ma_don_hang 
    HAVING COUNT(*) > 1
)
BEGIN
    -- Use a Common Table Expression to find duplicates and append a suffix
    WITH DuplicateOrders AS (
        SELECT id, ma_don_hang,
               ROW_NUMBER() OVER (PARTITION BY ma_don_hang ORDER BY id) as rn
        FROM HoaDon
        WHERE ma_don_hang IS NOT NULL
    )
    UPDATE HoaDon
    SET ma_don_hang = SUBSTRING(hd.ma_don_hang, 1, 30) + ''_DUP_'' + CAST(hd.id AS VARCHAR(20))
    FROM HoaDon hd
    JOIN DuplicateOrders do ON hd.id = do.id
    WHERE do.rn > 1;
END;
');

-- 4. Add Unique Constraint on ma_don_hang if it doesn't exist
IF NOT EXISTS (SELECT * FROM sys.objects WHERE name = 'UK_HOADON_MADONHANG' AND parent_object_id = OBJECT_ID('HoaDon'))
BEGIN
    ALTER TABLE HoaDon ADD CONSTRAINT UK_HOADON_MADONHANG UNIQUE(ma_don_hang);
END;

-- 5. Create index for ma_don_hang if not exists
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_HOADON_MA_DON_HANG' AND object_id = OBJECT_ID('HoaDon'))
BEGIN
    CREATE INDEX IDX_HOADON_MA_DON_HANG ON HoaDon(ma_don_hang);
END;

-- 6. Create PaymentTransaction table if not exists
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('PaymentTransaction') AND type = 'U')
BEGIN
    CREATE TABLE PaymentTransaction (
        id INT IDENTITY(1,1) PRIMARY KEY,
        transaction_id VARCHAR(100) NOT NULL UNIQUE,
        order_id INT NOT NULL,
        amount DECIMAL(18,2) NOT NULL,
        gateway VARCHAR(50) NOT NULL,
        status VARCHAR(50) NOT NULL,
        raw_payload NVARCHAR(MAX) NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        CONSTRAINT FK_PaymentTransaction_HoaDon FOREIGN KEY (order_id) REFERENCES HoaDon(id)
    );
END;

-- 7. Create indexes for PaymentTransaction
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_PAYMENT_TRANSACTION_ID' AND object_id = OBJECT_ID('PaymentTransaction'))
BEGIN
    -- Check if duplicates exist in case the table was created without UNIQUE constraint beforehand
    EXEC('
    IF EXISTS (
        SELECT transaction_id, COUNT(*) 
        FROM PaymentTransaction 
        GROUP BY transaction_id 
        HAVING COUNT(*) > 1
    )
    BEGIN
        WITH DuplicateTx AS (
            SELECT id, transaction_id,
                   ROW_NUMBER() OVER (PARTITION BY transaction_id ORDER BY id) as rn
            FROM PaymentTransaction
        )
        UPDATE PaymentTransaction
        SET transaction_id = SUBSTRING(pt.transaction_id, 1, 80) + ''_DUP_'' + CAST(pt.id AS VARCHAR(20))
        FROM PaymentTransaction pt
        JOIN DuplicateTx dt ON pt.id = dt.id
        WHERE dt.rn > 1;
    END;
    ');

    CREATE UNIQUE INDEX IDX_PAYMENT_TRANSACTION_ID ON PaymentTransaction(transaction_id);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_PAYMENT_ORDER_ID' AND object_id = OBJECT_ID('PaymentTransaction'))
BEGIN
    CREATE INDEX IDX_PAYMENT_ORDER_ID ON PaymentTransaction(order_id);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IDX_PAYMENT_CREATED_AT' AND object_id = OBJECT_ID('PaymentTransaction'))
BEGIN
    CREATE INDEX IDX_PAYMENT_CREATED_AT ON PaymentTransaction(created_at);
END;
