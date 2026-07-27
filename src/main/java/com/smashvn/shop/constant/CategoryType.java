package com.smashvn.shop.constant;

import com.smashvn.shop.entity.DanhMuc;

public enum CategoryType {
    VOT, GIAY, TRANG_PHUC, HOP_CAU, CUOC, BALO, QUAN_CAN, BANG_QUAN, OTHER;

    public static CategoryType fromDanhMuc(DanhMuc dm) {
        if (dm == null || dm.getTenDanhMuc() == null) return OTHER;
        String name = dm.getTenDanhMuc().toLowerCase().trim();
        if (name.contains("vợt") || name.contains("vot")) return VOT;
        if (name.contains("giày") || name.contains("giay")) return GIAY;
        if (name.contains("trang phục") || name.contains("quần") || name.contains("áo") || name.contains("trang phuc")) return TRANG_PHUC;
        if (name.contains("hộp cầu") || name.contains("quả cầu") || name.contains("hop cau")) return HOP_CAU;
        if (name.contains("cước") || name.contains("cuoc")) return CUOC;
        if (name.contains("balo") || name.contains("túi")) return BALO;
        if (name.contains("quấn cán") || name.contains("quan can")) return QUAN_CAN;
        if (name.contains("băng quấn") || name.contains("bang quan")) return BANG_QUAN;
        return OTHER;
    }

    public static CategoryType fromIdOrName(DanhMuc dm, Integer id) {
        if (id != null) {
            if (id == DanhMucIds.VOT) return VOT;
            if (id == DanhMucIds.GIAY) return GIAY;
            if (id == DanhMucIds.TRANG_PHUC) return TRANG_PHUC;
            if (id == DanhMucIds.HOP_CAU) return HOP_CAU;
            if (id == DanhMucIds.CUOC) return CUOC;
            if (id == DanhMucIds.BALO) return BALO;
            if (id == DanhMucIds.QUAN_CAN) return QUAN_CAN;
            if (id == DanhMucIds.BANG_QUAN) return BANG_QUAN;
        }
        return fromDanhMuc(dm);
    }
}
