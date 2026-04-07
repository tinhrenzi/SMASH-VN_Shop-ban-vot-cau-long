package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.repository.SoDiaChiRepository;

@Service
@RequiredArgsConstructor
public class UserAddressService {

    private final SoDiaChiRepository soDiaChiRepository;

    // 1. Lấy danh sách địa chỉ của 1 khách hàng
    public List<SoDiaChi> layDanhSachDiaChi(Integer idKhachHang) {
        // Dùng hàm bạn đã định nghĩa trong Repository
        return soDiaChiRepository.findByKhachHang_IdOrderByDefault(idKhachHang);
    }

    // 2. Thêm địa chỉ mới
    @Transactional
    public void themDiaChiMoi(KhachHang khachHang, String ho, String ten, String sdt, 
                              String diaChiCuThe, String tinhThanh, String quocGia, boolean isDefault) {
        
        SoDiaChi dc = new SoDiaChi();
        dc.setKhachHang(khachHang);
        dc.setHoNguoiNhan(ho);
        dc.setTenNguoiNhan(ten);
        dc.setSdtNguoiNhan(sdt);
        dc.setDiaChiCuThe(diaChiCuThe);
        dc.setTinhThanh(tinhThanh);
        dc.setThanhPho(tinhThanh); // Tạm gán giống Tỉnh thành
        dc.setQuocGia(quocGia);
        dc.setMaBuuDien("700000"); // Tạm gán mặc định nếu HTML không có ô nhập
        
        // Nếu chọn làm mặc định, phải gỡ mặc định của các địa chỉ cũ
        if (isDefault) {
            goDiaChiMacDinh(khachHang.getId());
            dc.setDefaultShipping(true);
            dc.setDefaultBilling(true);
        } else {
            // Kiểm tra nếu đây là địa chỉ ĐẦU TIÊN thì tự động cho nó làm mặc định luôn
            long count = soDiaChiRepository.countByKhachHang_IdAndDefaultShippingTrue(khachHang.getId());
            if (count == 0) {
                dc.setDefaultShipping(true);
                dc.setDefaultBilling(true);
            }
        }
        
        soDiaChiRepository.save(dc);
    }

    // Hàm phụ: Gỡ bỏ trạng thái mặc định của các địa chỉ cũ
    private void goDiaChiMacDinh(Integer idKhachHang) {
        List<SoDiaChi> list = soDiaChiRepository.findByKhachHang_Id(idKhachHang);
        for (SoDiaChi d : list) {
            d.setDefaultShipping(false);
            d.setDefaultBilling(false);
        }
        soDiaChiRepository.saveAll(list);
    }
 // 3. Đặt một địa chỉ làm mặc định
    @Transactional
    public void datLamMacDinh(Integer idDiaChi, Integer idKhachHang) {
        // Lấy địa chỉ từ DB
        SoDiaChi diaChiMoi = soDiaChiRepository.findById(idDiaChi)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));

        // BẢO MẬT: Đảm bảo địa chỉ này thuộc về người đang đăng nhập
        if (!diaChiMoi.getKhachHang().getId().equals(idKhachHang)) {
            throw new RuntimeException("Bạn không có quyền thay đổi địa chỉ này!");
        }

        // Tái sử dụng hàm gỡ mặc định cũ (đã viết ở bước trước)
        goDiaChiMacDinh(idKhachHang);

        // Đặt địa chỉ mới làm mặc định
        diaChiMoi.setDefaultShipping(true);
        diaChiMoi.setDefaultBilling(true);
        soDiaChiRepository.save(diaChiMoi);
    }
 // 4. Lấy 1 địa chỉ cụ thể để sửa (Có kiểm tra bảo mật)
    public SoDiaChi layDiaChiTheoId(Integer idDiaChi, Integer idKhachHang) {
        SoDiaChi dc = soDiaChiRepository.findById(idDiaChi)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));
                
        // BẢO MẬT: Chặn trường hợp user A cố tình gõ URL để sửa địa chỉ của user B
        if (!dc.getKhachHang().getId().equals(idKhachHang)) {
            throw new RuntimeException("Bạn không có quyền truy cập địa chỉ này!");
        }
        return dc;
    }

    // 5. Lưu cập nhật địa chỉ
    @Transactional
    public void capNhatDiaChi(Integer idDiaChi, Integer idKhachHang, String ho, String ten, String sdt, 
                              String diaChiCuThe, String tinhThanh, String quocGia, boolean isDefault) {
        
        SoDiaChi dc = layDiaChiTheoId(idDiaChi, idKhachHang); // Lấy địa chỉ cũ lên
        
        // Cập nhật thông tin mới
        dc.setHoNguoiNhan(ho);
        dc.setTenNguoiNhan(ten);
        dc.setSdtNguoiNhan(sdt);
        dc.setDiaChiCuThe(diaChiCuThe);
        dc.setTinhThanh(tinhThanh);
        dc.setThanhPho(tinhThanh); 
        dc.setQuocGia(quocGia);
        
        // Nếu người dùng tích chọn "Đặt làm mặc định" và địa chỉ này CHƯA phải là mặc định
        if (isDefault && !dc.isDefaultShipping()) {
            goDiaChiMacDinh(idKhachHang); // Gỡ các địa chỉ khác
            dc.setDefaultShipping(true);
            dc.setDefaultBilling(true);
        } 
        // Lưu ý: Nếu họ bỏ tích thì chúng ta không gỡ mặc định ở đây để tránh việc khách không có địa chỉ mặc định nào.
        
        soDiaChiRepository.save(dc);
    }
}