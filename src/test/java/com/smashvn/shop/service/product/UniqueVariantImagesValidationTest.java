package com.smashvn.shop.service.product;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smashvn.shop.entity.SanPham;
import com.smashvn.shop.repository.DanhMucRepository;
import com.smashvn.shop.repository.SanPhamRepository;
import com.smashvn.shop.repository.ThuongHieuRepository;
import com.smashvn.shop.service.admin.AdminSanPhamService;
import com.smashvn.shop.service.admin.AdminBienTheService;

@SpringBootTest
@Transactional
public class UniqueVariantImagesValidationTest {

    @Autowired
    private AdminSanPhamService adminSanPhamService;

    @Autowired
    private AdminBienTheService adminBienTheService;

    @Autowired
    private SanPhamRepository sanPhamRepository;

    @Autowired
    private DanhMucRepository danhMucRepository;

    @Autowired
    private ThuongHieuRepository thuongHieuRepository;

    @Value("${app.upload.path}")
    private String uploadPathConfig;

    private Path productDir;
    private final List<Path> createdFiles = new ArrayList<>();

    @BeforeEach
    public void setUp() throws IOException {
        productDir = Paths.get(uploadPathConfig).resolve("product").normalize().toAbsolutePath();
        if (!Files.exists(productDir)) {
            Files.createDirectories(productDir);
        }
    }

    @AfterEach
    public void tearDown() {
        for (Path p : createdFiles) {
            try {
                Files.deleteIfExists(p);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private byte[] createDummyPng(int sizeOffset) {
        try {
            java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(10 + sizeOffset, 10, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testUploadImagePreservesOriginalVietnameseNameAndDeduplicates() throws Exception {
        String testName = "Axforce Thunder Cannon_Xanh Đậm.png";
        byte[] content1 = createDummyPng(100);
        MockMultipartFile file = new MockMultipartFile("fileAnh", testName, "image/png", content1);

        // Define target path
        Path targetPath = productDir.resolve(testName).normalize();
        Files.deleteIfExists(targetPath); // ensure clean start

        // Unwrap the proxy to get the real target instance (avoids null fields in proxy instance)
        Object targetService = org.springframework.test.util.AopTestUtils.getTargetObject(adminSanPhamService);
        
        java.lang.reflect.Method saveMethod = AdminSanPhamService.class.getDeclaredMethod("saveImageSecurely", MultipartFile.class, String.class, List.class);
        saveMethod.setAccessible(true);
        List<Path> uploaded = new ArrayList<>();
        
        String savedName = (String) saveMethod.invoke(targetService, file, "test", uploaded);
        createdFiles.add(targetPath);

        assertEquals(testName, savedName);
        assertTrue(Files.exists(targetPath));
        assertEquals(1, uploaded.size()); // should be tracked for rollback since it is newly written

        // 2. Second upload with same name and content
        List<Path> uploaded2 = new ArrayList<>();
        String savedName2 = (String) saveMethod.invoke(targetService, file, "test", uploaded2);
        assertEquals(testName, savedName2);
        assertEquals(0, uploaded2.size()); // should NOT be tracked since it is reused

        // 3. Third upload with same name but different content (should throw exception)
        byte[] content2 = createDummyPng(5); // size difference changes hash
        MockMultipartFile fileDiff = new MockMultipartFile("fileAnh", testName, "image/png", content2);
        
        try {
            saveMethod.invoke(targetService, fileDiff, "test", new ArrayList<Path>());
            fail("Expected exception for same name but different content");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            assertTrue(cause.getMessage().contains("Đã tồn tại ảnh có tên") && cause.getMessage().contains("nội dung khác"));
        }
    }

    @Test
    public void testPathTraversalIsSanitized() throws Exception {
        String unsafeName = "../../../unsafe_image.png";
        byte[] content = createDummyPng(100);
        MockMultipartFile file = new MockMultipartFile("fileAnh", unsafeName, "image/png", content);

        Object targetService = org.springframework.test.util.AopTestUtils.getTargetObject(adminSanPhamService);
        java.lang.reflect.Method saveMethod = AdminSanPhamService.class.getDeclaredMethod("saveImageSecurely", MultipartFile.class, String.class, List.class);
        saveMethod.setAccessible(true);
        List<Path> uploaded = new ArrayList<>();

        String savedName = (String) saveMethod.invoke(targetService, file, "test", uploaded);
        
        // Unsafe path components must be removed by Paths.get().getFileName()
        assertEquals("unsafe_image.png", savedName);
        Path targetPath = productDir.resolve("unsafe_image.png").normalize();
        createdFiles.add(targetPath);
        assertTrue(Files.exists(targetPath));
    }
}
