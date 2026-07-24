package com.smashvn.shop.dto;

import java.math.BigDecimal;
import lombok.Data;

/**
 * DTO đại diện cho dữ liệu của từng biến thể chi tiết (variants[i]) khi gửi từ Form thêm mới sản phẩm.
 * Chứa thông tin màu sắc, trọng lượng (cho vợt), kích thước (cho giày/áo), giá nhập, giá bán, tồn kho và chỉ số ảnh màu (colorIndex).
 */
@Data
public class BienTheCreateRequest {
    private String mauSac;
    private String trongLuong;
    private String kichThuoc;
    private BigDecimal giaNhap;
    private BigDecimal giaBan;
    private Integer soLuongTon;
    private Integer colorIndex;
}
