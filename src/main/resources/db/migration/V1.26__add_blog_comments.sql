-- Flyway Migration V1.26: Create BlogComment and CommentModerationKeyword tables with Hardening constraints
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('CommentModerationKeyword') AND type = 'U')
BEGIN
    CREATE TABLE CommentModerationKeyword (
        id INT IDENTITY(1,1) PRIMARY KEY,
        keyword NVARCHAR(255) NOT NULL UNIQUE,
        active BIT NOT NULL DEFAULT 1,
        created_at DATETIME NOT NULL DEFAULT GETDATE()
    );
    -- Chèn một số từ khóa cấm mẫu để phục vụ kiểm thử
    INSERT INTO CommentModerationKeyword (keyword, active) VALUES (N'đm', 1);
    INSERT INTO CommentModerationKeyword (keyword, active) VALUES (N'dmm', 1);
    INSERT INTO CommentModerationKeyword (keyword, active) VALUES (N'vkl', 1);
    INSERT INTO CommentModerationKeyword (keyword, active) VALUES (N'địt', 1);
    INSERT INTO CommentModerationKeyword (keyword, active) VALUES (N'đĩ', 1);
    INSERT INTO CommentModerationKeyword (keyword, active) VALUES (N'vãi', 1);
    INSERT INTO CommentModerationKeyword (keyword, active) VALUES (N'cặc', 1);
END;

IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID('BlogComment') AND type = 'U')
BEGIN
    CREATE TABLE BlogComment (
        id INT IDENTITY(1,1) PRIMARY KEY,
        id_blog INT NOT NULL,
        id_tai_khoan INT NOT NULL,
        content NVARCHAR(1000) NOT NULL,
        created_at DATETIME NOT NULL DEFAULT GETDATE(),
        created_by INT NULL, -- Lưu vết tài khoản tạo bình luận
        deleted BIT NOT NULL DEFAULT 0,
        deleted_at DATETIME NULL,
        deleted_by INT NULL, -- Lưu vết admin thực hiện xóa
        deleted_reason NVARCHAR(500) NULL, -- Lý do xóa bình luận
        parent_comment_id INT NULL, -- Dự phòng cho tương lai (threaded comment replies)
        CONSTRAINT FK_BlogComment_Blog FOREIGN KEY (id_blog) REFERENCES Blog(id) ON DELETE CASCADE,
        CONSTRAINT FK_BlogComment_TaiKhoan FOREIGN KEY (id_tai_khoan) REFERENCES TaiKhoan(id) ON DELETE NO ACTION,
        CONSTRAINT FK_BlogComment_CreatedBy FOREIGN KEY (created_by) REFERENCES TaiKhoan(id) ON DELETE NO ACTION,
        CONSTRAINT FK_BlogComment_DeletedBy FOREIGN KEY (deleted_by) REFERENCES TaiKhoan(id) ON DELETE NO ACTION,
        CONSTRAINT FK_BlogComment_Parent FOREIGN KEY (parent_comment_id) REFERENCES BlogComment(id) ON DELETE NO ACTION,
        CONSTRAINT CK_BlogComment_Content_Length CHECK (LEN(LTRIM(RTRIM(content))) BETWEEN 1 AND 1000) -- Ràng buộc CHECK không cho phép chỉ chứa khoảng trắng
    );
END;

-- Tối ưu hóa hiệu năng đếm và lấy bình luận của một bài viết theo thời gian
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BlogComment_Blog_Deleted_CreatedAt' AND object_id = OBJECT_ID('BlogComment'))
BEGIN
    CREATE INDEX IX_BlogComment_Blog_Deleted_CreatedAt 
    ON BlogComment(id_blog, deleted, created_at DESC);
END;

-- Tối ưu hóa câu truy vấn kiểm tra spam của người dùng
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BlogComment_User_CreatedAt' AND object_id = OBJECT_ID('BlogComment'))
BEGIN
    CREATE INDEX IX_BlogComment_User_CreatedAt 
    ON BlogComment(id_tai_khoan, created_at DESC);
END;

-- Tối ưu hóa lọc bình luận chưa xóa khi số lượng bản ghi lớn
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BlogComment_Deleted' AND object_id = OBJECT_ID('BlogComment'))
BEGIN
    CREATE INDEX IX_BlogComment_Deleted ON BlogComment(deleted);
END;

-- Tối ưu hóa các truy vấn kiểm tra sự tồn tại hoặc đếm cơ bản của bài viết
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_BlogComment_Blog_Active' AND object_id = OBJECT_ID('BlogComment'))
BEGIN
    CREATE INDEX IX_BlogComment_Blog_Active ON BlogComment(id_blog, deleted);
END;
