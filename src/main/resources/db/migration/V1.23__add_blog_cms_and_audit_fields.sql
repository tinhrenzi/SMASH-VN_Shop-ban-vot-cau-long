-- Flyway migration V1.23
-- Add CMS, soft-delete, and audit fields to Blog table
ALTER TABLE Blog ADD status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE Blog ADD deleted BIT NOT NULL DEFAULT 0;
ALTER TABLE Blog ADD deleted_at DATETIME NULL;
ALTER TABLE Blog ADD created_by VARCHAR(255) NULL;
ALTER TABLE Blog ADD created_at DATETIME NOT NULL DEFAULT GETDATE();
ALTER TABLE Blog ADD updated_by VARCHAR(255) NULL;
ALTER TABLE Blog ADD updated_at DATETIME NULL;
GO

-- Enforce CHECK constraint on status
ALTER TABLE Blog
ADD CONSTRAINT CK_BLOG_STATUS
CHECK (status IN ('DRAFT', 'PUBLISHED'));
GO

-- Create performance indexes
CREATE INDEX IX_BLOG_STATUS ON Blog(status);
CREATE INDEX IX_BLOG_DELETED ON Blog(deleted);
CREATE INDEX IX_BLOG_PUBLISH_DATE ON Blog(publish_date);
GO
