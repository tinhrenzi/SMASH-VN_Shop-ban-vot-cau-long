-- 1. Dynamic script to drop all Check constraints on DanhGia.so_sao
DECLARE @SqlCheck NVARCHAR(MAX) = '';
SELECT @SqlCheck = @SqlCheck + 'ALTER TABLE DanhGia DROP CONSTRAINT ' + QUOTENAME(cc.name) + ';' + CHAR(13)
FROM sys.check_constraints cc
JOIN sys.columns c ON cc.parent_object_id = c.object_id AND cc.parent_column_id = c.column_id
WHERE cc.parent_object_id = OBJECT_ID('DanhGia') AND c.name = 'so_sao';

IF @SqlCheck <> ''
BEGIN
    EXEC sp_executesql @SqlCheck;
END;

-- 2. Dynamic script to drop all Default constraints on DanhGia.so_sao
DECLARE @SqlDefault NVARCHAR(MAX) = '';
SELECT @SqlDefault = @SqlDefault + 'ALTER TABLE DanhGia DROP CONSTRAINT ' + QUOTENAME(dc.name) + ';' + CHAR(13)
FROM sys.default_constraints dc
JOIN sys.columns c ON dc.parent_object_id = c.object_id AND dc.parent_column_id = c.column_id
WHERE dc.parent_object_id = OBJECT_ID('DanhGia') AND c.name = 'so_sao';

IF @SqlDefault <> ''
BEGIN
    EXEC sp_executesql @SqlDefault;
END;

-- 3. Alter column to FLOAT NOT NULL
ALTER TABLE DanhGia ALTER COLUMN so_sao FLOAT NOT NULL;

-- 4. Re-add check constraint for so_sao between 1 and 5
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'CK_DanhGia_SoSao')
BEGIN
    ALTER TABLE DanhGia ADD CONSTRAINT CK_DanhGia_SoSao CHECK (so_sao >= 1.0 AND so_sao <= 5.0);
END;

