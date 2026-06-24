-- SQL Server 2022 Identity Synchronization and Reseed Utility
SET NOCOUNT ON;
SET XACT_ABORT ON;
-- Configuration:
-- @DryRun = 1 -> Print generated commands only (Safe-testing mode)
-- @DryRun = 0 -> Execute generated commands
DECLARE @DryRun BIT = 1;
DECLARE @schema_name SYSNAME;
DECLARE @table_name SYSNAME;
DECLARE @col_name SYSNAME;
DECLARE @sql NVARCHAR(MAX);
DECLARE @max_id BIGINT;
DECLARE @target_reseed BIGINT;
DECLARE @msg NVARCHAR(2000);
-- 1. Scan user tables using sys.identity_columns for SQL Server 2022 compatibility
DECLARE db_cursor CURSOR LOCAL FOR
SELECT s.name AS schema_name,
    t.name AS table_name,
    c.name AS column_name
FROM sys.tables t
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    JOIN sys.identity_columns c ON c.object_id = t.object_id
WHERE t.is_ms_shipped = 0;
OPEN db_cursor;
FETCH NEXT
FROM db_cursor INTO @schema_name,
    @table_name,
    @col_name;
WHILE @@FETCH_STATUS = 0 BEGIN -- 2. Query max ID dynamically (using QUOTENAME for safety in SELECT)
SET @sql = N'SELECT @max_id_out = MAX(' + QUOTENAME(@col_name) + N') FROM ' + QUOTENAME(@schema_name) + N'.' + QUOTENAME(@table_name);
EXEC sp_executesql @sql,
N '@max_id_out BIGINT OUTPUT',
@max_id_out = @max_id OUTPUT;
-- 3. Reseed target determination
IF @max_id IS NULL
SET @target_reseed = 0;
ELSE
SET @target_reseed = @max_id;
-- 4. Construct DBCC CHECKIDENT using schema.table format (no brackets)
SET @sql = N 'DBCC CHECKIDENT (''' + @schema_name + N'.' + @table_name + N'' ', RESEED, ' + CAST(@target_reseed AS NVARCHAR(20)) + N')';
-- 5. Execution or Dry-Run Mode print
IF @DryRun = 1 BEGIN
SET @msg = N'DryRun - Generated command: ' + @sql;
RAISERROR(@msg, 0, 1) WITH NOWAIT;
END
ELSE BEGIN
SET @msg = N'Executing: ' + @sql;
RAISERROR(@msg, 0, 1) WITH NOWAIT;
EXEC(@sql);
END FETCH NEXT
FROM db_cursor INTO @schema_name,
    @table_name,
    @col_name;
END;
CLOSE db_cursor;
DEALLOCATE db_cursor;
-- 6. Output Summary Statistics
SELECT COUNT(*) AS TotalIdentityTablesProcessed
FROM sys.identity_columns ic
    JOIN sys.tables t ON t.object_id = ic.object_id
WHERE t.is_ms_shipped = 0;
-- 7. Output Detailed Verification Report
SELECT DB_NAME() AS DatabaseName,
    s.name AS SchemaName,
    t.name AS TableName,
    c.name AS ColumnName,
    c.last_value AS LastIdentityValue
FROM sys.tables t
    JOIN sys.schemas s ON s.schema_id = t.schema_id
    JOIN sys.identity_columns c ON c.object_id = t.object_id
WHERE t.is_ms_shipped = 0;