package com.smashvn.shop.service.common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.upload.path}")
    private String uploadPathConfig;

    private final Tika tika = new Tika();

    /**
     * Tải lên danh sách hình ảnh một cách an toàn và trả về danh sách tên tệp ngẫu nhiên
     */
    public List<String> saveReviewImages(List<MultipartFile> files) throws Exception {
        List<String> savedFileNames = new ArrayList<>();
        
        if (files == null || files.isEmpty()) {
            return savedFileNames;
        }

        // Loại bỏ các tệp rỗng/không được chọn
        List<MultipartFile> activeFiles = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                activeFiles.add(file);
            }
        }

        if (activeFiles.isEmpty()) {
            return savedFileNames;
        }

        // 1. Kiểm tra tối đa 5 hình ảnh
        if (activeFiles.size() > 5) {
            throw new IllegalArgumentException("Bạn chỉ được tải lên tối đa 5 hình ảnh cho mỗi đánh giá.");
        }

        // 2. Kiểm tra tổng dung lượng tải lên (tối đa 20 MB)
        long totalSize = 0;
        for (MultipartFile file : activeFiles) {
            totalSize += file.getSize();
        }
        if (totalSize > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("Tổng dung lượng các hình ảnh tải lên không được vượt quá 20MB.");
        }

        // 3. Tiến hành xử lý từng tệp
        for (MultipartFile file : activeFiles) {
            // Kiểm tra dung lượng đơn lẻ (tối đa 5 MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Kích thước mỗi hình ảnh không được vượt quá 5MB.");
            }

            // Kiểm tra đuôi mở rộng
            String origName = file.getOriginalFilename();
            String ext = "";
            if (origName != null && origName.contains(".")) {
                ext = origName.substring(origName.lastIndexOf(".") + 1).toLowerCase();
            }
            if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png") && !ext.equals("webp")) {
                throw new IllegalArgumentException("Định dạng tệp không hợp lệ! Chỉ cho phép JPG, JPEG, PNG, WEBP.");
            }

            // Kiểm tra MIME type thực tế bằng Apache Tika
            try (InputStream is = file.getInputStream()) {
                String mimeType = tika.detect(is);
                if (mimeType == null || (!mimeType.equals("image/jpeg") && !mimeType.equals("image/png") && !mimeType.equals("image/webp"))) {
                    throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ! MIME type không được chấp nhận.");
                }
            }

            // Kiểm tra nội dung hình ảnh thực tế bằng ImageIO (ngăn chặn chèn mã độc/giả mạo exe)
            try (InputStream is = file.getInputStream()) {
                java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(is);
                if (image == null) {
                    throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Tệp tải lên không phải là ảnh hợp lệ!");
            }

            // Tạo tên file UUID ngẫu nhiên tránh trùng lặp
            String secureFileName = UUID.randomUUID().toString() + "." + ext;

            // Xác định thư mục lưu trữ và chống Path Traversal
            Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
            Path reviewsUploadPath = rootUploadPath.resolve("reviews").normalize();
            if (!Files.exists(reviewsUploadPath)) {
                Files.createDirectories(reviewsUploadPath);
            }

            Path targetFilePath = reviewsUploadPath.resolve(secureFileName).normalize().toAbsolutePath();
            if (!targetFilePath.startsWith(reviewsUploadPath.toAbsolutePath().normalize())) {
                throw new SecurityException("Đường dẫn tải lên tệp không hợp lệ (ngăn chặn Path Traversal).");
            }

            // Lưu tệp lên đĩa
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                log.error("[UPLOAD_FAILURE] Failed to save review image: {}", e.getMessage());
                // Xóa các file đã upload trước đó trong lượt này nếu bị lỗi giữa chừng
                for (String name : savedFileNames) {
                    deleteImage(name, "reviews");
                }
                throw e;
            }

            savedFileNames.add(secureFileName);
        }

        return savedFileNames;
    }

    /**
     * Xóa tệp hình ảnh vật lý trên đĩa
     */
    public void deleteImage(String fileName, String folderName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        try {
            Path rootUploadPath = Paths.get(uploadPathConfig).toAbsolutePath().normalize();
            Path targetFolder = rootUploadPath.resolve(folderName).normalize();
            Path filePath = targetFolder.resolve(fileName).normalize().toAbsolutePath();

            // Chống xóa ngoài thư mục cho phép (Path Traversal)
            if (filePath.startsWith(targetFolder.toAbsolutePath().normalize())) {
                Files.deleteIfExists(filePath);
                log.info("[FILE_CLEANUP] Deleted image file: {}", filePath);
            }
        } catch (Exception e) {
            log.error("[FILE_CLEANUP_ERROR] Failed to delete image file {}: {}", fileName, e.getMessage());
        }
    }
}
