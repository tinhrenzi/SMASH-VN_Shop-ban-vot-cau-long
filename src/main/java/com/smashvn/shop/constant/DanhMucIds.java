package com.smashvn.shop.constant;

/**
 * Class hằng số quản lý các ID danh mục thực tế từ cơ sở dữ liệu.
 * Dùng để phân loại nghiệp vụ sản phẩm (Vợt, Giày, Hộp cầu, Trang phục, v.v.)
 * tại Tầng Service và Controller mà không cần hard-code ID rải rác.
 */
public final class DanhMucIds {
    public static final int VOT        = 42;
    public static final int BALO       = 43;
    public static final int GIAY       = 464;
    public static final int HOP_CAU    = 465;
    public static final int CUOC       = 466;
    public static final int TRANG_PHUC = 467;
    public static final int QUAN_CAN   = 468;
    public static final int BANG_QUAN  = 469;

    private DanhMucIds() {}

    public static boolean isSupported(int idDanhMuc) {
        return idDanhMuc == VOT || idDanhMuc == BALO || idDanhMuc == GIAY
            || idDanhMuc == HOP_CAU || idDanhMuc == CUOC || idDanhMuc == TRANG_PHUC
            || idDanhMuc == QUAN_CAN || idDanhMuc == BANG_QUAN;
    }
}
