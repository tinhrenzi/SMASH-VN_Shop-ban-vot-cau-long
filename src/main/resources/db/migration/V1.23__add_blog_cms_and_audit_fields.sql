-- Flyway migration V1.23
-- Add CMS, soft-delete, and audit fields to Blog table

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'status')
BEGIN
    ALTER TABLE Blog ADD status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'deleted')
BEGIN
    ALTER TABLE Blog ADD deleted BIT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'deleted_at')
BEGIN
    ALTER TABLE Blog ADD deleted_at DATETIME NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'created_by')
BEGIN
    ALTER TABLE Blog ADD created_by VARCHAR(255) NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'created_at')
BEGIN
    ALTER TABLE Blog ADD created_at DATETIME NOT NULL DEFAULT GETDATE();
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'updated_by')
BEGIN
    ALTER TABLE Blog ADD updated_by VARCHAR(255) NULL;
END;

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('Blog') AND name = 'updated_at')
BEGIN
    ALTER TABLE Blog ADD updated_at DATETIME NULL;
END;
GO

-- Enforce CHECK constraint on status
IF NOT EXISTS (SELECT * FROM sys.check_constraints WHERE name = 'CK_BLOG_STATUS' AND parent_object_id = OBJECT_ID('Blog'))
BEGIN
    ALTER TABLE Blog
    ADD CONSTRAINT CK_BLOG_STATUS
    CHECK (status IN ('DRAFT', 'PUBLISHED'));
END;
GO

-- Create performance indexes
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BLOG_STATUS' AND object_id = OBJECT_ID('Blog'))
BEGIN
    CREATE INDEX IX_BLOG_STATUS ON Blog(status);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BLOG_DELETED' AND object_id = OBJECT_ID('Blog'))
BEGIN
    CREATE INDEX IX_BLOG_DELETED ON Blog(deleted);
END;

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BLOG_PUBLISH_DATE' AND object_id = OBJECT_ID('Blog'))
BEGIN
    CREATE INDEX IX_BLOG_PUBLISH_DATE ON Blog(publish_date);
END;
GO
