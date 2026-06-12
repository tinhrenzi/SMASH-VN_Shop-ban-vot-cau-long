package com.smashvn.shop.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import com.smashvn.shop.entity.KhachHang;
import com.smashvn.shop.entity.SoDiaChi;
import com.smashvn.shop.repository.SoDiaChiRepository;
import com.smashvn.shop.dto.UserAddressDto;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAddressService {

    private final SoDiaChiRepository soDiaChiRepository;

    // 1. Lấy danh sách địa chỉ của 1 khách hàng
    public List<SoDiaChi> layDanhSachDiaChi(Integer idKhachHang) {
        return soDiaChiRepository.findByKhachHang_IdOrderByDefault(idKhachHang);
    }

    // 2. Thêm địa chỉ mới
    @Transactional
    public void themDiaChiMoi(KhachHang khachHang, UserAddressDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu địa chỉ không hợp lệ.");
        }

        // 1. Trim inputs
        String ho = dto.getHoNguoiNhan() != null ? dto.getHoNguoiNhan().trim() : null;
        String ten = dto.getTenNguoiNhan() != null ? dto.getTenNguoiNhan().trim() : null;
        String sdt = dto.getSdtNguoiNhan() != null ? dto.getSdtNguoiNhan().trim() : null;
        String diaChiCuThe = dto.getDiaChiCuThe() != null ? dto.getDiaChiCuThe().trim() : null;
        String tinhThanh = dto.getTinhThanh() != null ? dto.getTinhThanh().trim() : null;
        String quocGia = dto.getQuocGia() != null ? dto.getQuocGia().trim() : null;

        // 2. Sanitize inputs against XSS using Jsoup
        String sanitizedHo = sanitizeInput(ho);
        String sanitizedTen = sanitizeInput(ten);
        String sanitizedSdt = sanitizeInput(sdt);
        String sanitizedDiaChiCuThe = sanitizeInput(diaChiCuThe);
        String sanitizedTinhThanh = sanitizeInput(tinhThanh);
        String sanitizedQuocGia = sanitizeInput(quocGia);

        if ((ho != null && !ho.equals(sanitizedHo)) || 
            (ten != null && !ten.equals(sanitizedTen)) || 
            (sdt != null && !sdt.equals(sanitizedSdt)) || 
            (diaChiCuThe != null && !diaChiCuThe.equals(sanitizedDiaChiCuThe)) || 
            (tinhThanh != null && !tinhThanh.equals(sanitizedTinhThanh)) || 
            (quocGia != null && !quocGia.equals(sanitizedQuocGia))) {
            log.warn("[SECURITY_ALERT] XSS payload detected and sanitized in address submission for customer id: {}", khachHang.getId());
        }

        // 3. Validate empty/blank/null and length limits on sanitized values
        validateAddressFields(sanitizedHo, sanitizedTen, sanitizedSdt, sanitizedDiaChiCuThe, sanitizedTinhThanh, sanitizedQuocGia, khachHang.getId());

        SoDiaChi dc = new SoDiaChi();
        dc.setKhachHang(khachHang);
        dc.setHoNguoiNhan(sanitizedHo);
        dc.setTenNguoiNhan(sanitizedTen);
        dc.setSdtNguoiNhan(sanitizedSdt);
        dc.setDiaChiCuThe(sanitizedDiaChiCuThe);
        dc.setTinhThanh(sanitizedTinhThanh);
        dc.setThanhPho(sanitizedTinhThanh); // Tạm gán giống Tỉnh thành
        dc.setQuocGia(sanitizedQuocGia);
        dc.setMaBuuDien("700000"); // Tạm gán mặc định
        dc.setLatitude(dto.getLatitude());
        dc.setLongitude(dto.getLongitude());
        
        // Nếu chọn làm mặc định, phải gỡ mặc định của các địa chỉ cũ
        if (dto.isDefaultAddress()) {
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
        SoDiaChi diaChiMoi = soDiaChiRepository.findById(idDiaChi)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));

        if (!diaChiMoi.getKhachHang().getId().equals(idKhachHang)) {
            throw new RuntimeException("Bạn không có quyền thay đổi địa chỉ này!");
        }

        goDiaChiMacDinh(idKhachHang);

        diaChiMoi.setDefaultShipping(true);
        diaChiMoi.setDefaultBilling(true);
        soDiaChiRepository.save(diaChiMoi);
    }

    // 4. Lấy 1 địa chỉ cụ thể để sửa (Có kiểm tra bảo mật)
    public SoDiaChi layDiaChiTheoId(Integer idDiaChi, Integer idKhachHang) {
        SoDiaChi dc = soDiaChiRepository.findById(idDiaChi)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));
                
        if (!dc.getKhachHang().getId().equals(idKhachHang)) {
            throw new RuntimeException("Bạn không có quyền truy cập địa chỉ này!");
        }
        return dc;
    }

    // 5. Lưu cập nhật địa chỉ
    @Transactional
    public void capNhatDiaChi(Integer idDiaChi, Integer idKhachHang, UserAddressDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu địa chỉ không hợp lệ.");
        }

        // 1. Trim inputs
        String ho = dto.getHoNguoiNhan() != null ? dto.getHoNguoiNhan().trim() : null;
        String ten = dto.getTenNguoiNhan() != null ? dto.getTenNguoiNhan().trim() : null;
        String sdt = dto.getSdtNguoiNhan() != null ? dto.getSdtNguoiNhan().trim() : null;
        String diaChiCuThe = dto.getDiaChiCuThe() != null ? dto.getDiaChiCuThe().trim() : null;
        String tinhThanh = dto.getTinhThanh() != null ? dto.getTinhThanh().trim() : null;
        String quocGia = dto.getQuocGia() != null ? dto.getQuocGia().trim() : null;

        // 2. Sanitize inputs against XSS using Jsoup
        String sanitizedHo = sanitizeInput(ho);
        String sanitizedTen = sanitizeInput(ten);
        String sanitizedSdt = sanitizeInput(sdt);
        String sanitizedDiaChiCuThe = sanitizeInput(diaChiCuThe);
        String sanitizedTinhThanh = sanitizeInput(tinhThanh);
        String sanitizedQuocGia = sanitizeInput(quocGia);

        if ((ho != null && !ho.equals(sanitizedHo)) || 
            (ten != null && !ten.equals(sanitizedTen)) || 
            (sdt != null && !sdt.equals(sanitizedSdt)) || 
            (diaChiCuThe != null && !diaChiCuThe.equals(sanitizedDiaChiCuThe)) || 
            (tinhThanh != null && !tinhThanh.equals(sanitizedTinhThanh)) || 
            (quocGia != null && !quocGia.equals(sanitizedQuocGia))) {
            log.warn("[SECURITY_ALERT] XSS payload detected and sanitized in address edit for customer id: {}", idKhachHang);
        }

        // 3. Validate empty/blank/null and length limits on sanitized values
        validateAddressFields(sanitizedHo, sanitizedTen, sanitizedSdt, sanitizedDiaChiCuThe, sanitizedTinhThanh, sanitizedQuocGia, idKhachHang);

        SoDiaChi dc = layDiaChiTheoId(idDiaChi, idKhachHang); // Lấy địa chỉ cũ lên
        
        // Cập nhật thông tin mới
        dc.setHoNguoiNhan(sanitizedHo);
        dc.setTenNguoiNhan(sanitizedTen);
        dc.setSdtNguoiNhan(sanitizedSdt);
        dc.setDiaChiCuThe(sanitizedDiaChiCuThe);
        dc.setTinhThanh(sanitizedTinhThanh);
        dc.setThanhPho(sanitizedTinhThanh); 
        dc.setQuocGia(sanitizedQuocGia);
        dc.setLatitude(dto.getLatitude());
        dc.setLongitude(dto.getLongitude());
        
        // Nếu người dùng tích chọn "Đặt làm mặc định" và địa chỉ này CHƯA phải là mặc định
        if (dto.isDefaultAddress() && !dc.isDefaultShipping()) {
            goDiaChiMacDinh(idKhachHang); // Gỡ các địa chỉ khác
            dc.setDefaultShipping(true);
            dc.setDefaultBilling(true);
        } 
        
        soDiaChiRepository.save(dc);
    }

    // 6. Xóa địa chỉ (Có kiểm tra logic)
    @Transactional
    public void xoaDiaChi(Integer idDiaChi, Integer idKhachHang) {
        SoDiaChi dc = soDiaChiRepository.findById(idDiaChi)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ!"));

        if (!dc.getKhachHang().getId().equals(idKhachHang)) {
            throw new RuntimeException("Bạn không có quyền xóa địa chỉ này!");
        }

        if (dc.isDefaultShipping()) {
            List<SoDiaChi> danhSach = layDanhSachDiaChi(idKhachHang);
            if (danhSach.size() > 1) {
                throw new RuntimeException("Không thể xóa địa chỉ mặc định. Vui lòng chọn địa chỉ khác làm mặc định trước!");
            }
        }

        soDiaChiRepository.delete(dc);
    }

    private String sanitizeInput(String input) {
        if (input == null) return null;
        return Jsoup.clean(input, Safelist.none());
    }

    private void validateAddressFields(String ho, String ten, String sdt, String diaChi, String tinhThanh, String quocGia, Integer idKhachHang) {
        if (ho == null || ho.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'hoNguoiNhan' for customer id: {}", idKhachHang);
            throw new IllegalArgumentException("Họ người nhận không được để trống.");
        }
        if (ho.length() > 50) {
            log.warn("[SECURITY_ALERT] Invalid length of 'hoNguoiNhan' ({}) for customer id: {}", ho.length(), idKhachHang);
            throw new IllegalArgumentException("Họ người nhận không được vượt quá 50 ký tự.");
        }

        if (ten == null || ten.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'tenNguoiNhan' for customer id: {}", idKhachHang);
            throw new IllegalArgumentException("Tên người nhận không được để trống.");
        }
        if (ten.length() > 50) {
            log.warn("[SECURITY_ALERT] Invalid length of 'tenNguoiNhan' ({}) for customer id: {}", ten.length(), idKhachHang);
            throw new IllegalArgumentException("Tên người nhận không được vượt quá 50 ký tự.");
        }

        if (sdt == null || sdt.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'sdtNguoiNhan' for customer id: {}", idKhachHang);
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (!sdt.matches("^(0|\\+84)[0-9]{9}$")) {
            log.warn("[SECURITY_ALERT] Invalid phone format '{}' for customer id: {}", sdt, idKhachHang);
            throw new IllegalArgumentException("Số điện thoại không đúng định dạng!");
        }

        if (diaChi == null || diaChi.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'diaChiCuThe' for customer id: {}", idKhachHang);
            throw new IllegalArgumentException("Địa chỉ cụ thể không được để trống.");
        }
        if (diaChi.length() < 5 || diaChi.length() > 255) {
            log.warn("[SECURITY_ALERT] Invalid length of 'diaChiCuThe' ({}) for customer id: {}", diaChi.length(), idKhachHang);
            throw new IllegalArgumentException("Địa chỉ cụ thể phải từ 5 đến 255 ký tự.");
        }

        if (tinhThanh == null || tinhThanh.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'tinhThanh' for customer id: {}", idKhachHang);
            throw new IllegalArgumentException("Tỉnh/Thành phố không được để trống.");
        }
        if (tinhThanh.length() > 100) {
            log.warn("[SECURITY_ALERT] Invalid length of 'tinhThanh' ({}) for customer id: {}", tinhThanh.length(), idKhachHang);
            throw new IllegalArgumentException("Tỉnh/Thành phố không được vượt quá 100 ký tự.");
        }

        if (quocGia == null || quocGia.isEmpty()) {
            log.warn("[SECURITY_ALERT] Invalid empty 'quocGia' for customer id: {}", idKhachHang);
            throw new IllegalArgumentException("Quốc gia không được để trống.");
        }
        if (quocGia.length() > 100) {
            log.warn("[SECURITY_ALERT] Invalid length of 'quocGia' ({}) for customer id: {}", quocGia.length(), idKhachHang);
            throw new IllegalArgumentException("Quốc gia không được vượt quá 100 ký tự.");
        }
    }
}