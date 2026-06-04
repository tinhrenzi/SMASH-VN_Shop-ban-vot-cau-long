-- V1.5: Add POS payment method and notes fields to HoaDon
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'phuong_thuc_pos')
BEGIN
    ALTER TABLE HoaDon ADD phuong_thuc_pos NVARCHAR(20) NULL;
END;
GO

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('HoaDon') AND name = 'ghi_chu')
BEGIN
    ALTER TABLE HoaDon ADD ghi_chu NVARCHAR(500) NULL;
END;
GO
