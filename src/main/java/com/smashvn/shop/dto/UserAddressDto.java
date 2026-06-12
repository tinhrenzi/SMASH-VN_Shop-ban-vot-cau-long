package com.smashvn.shop.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAddressDto {
    private Integer id;

    @NotBlank(message = "Họ người nhận không được để trống!")
    @Size(max = 50, message = "Họ người nhận không được vượt quá 50 ký tự!")
    private String hoNguoiNhan;

    @NotBlank(message = "Tên người nhận không được để trống!")
    @Size(max = 50, message = "Tên người nhận không được vượt quá 50 ký tự!")
    private String tenNguoiNhan;

    @NotBlank(message = "Số điện thoại không được để trống!")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Số điện thoại không đúng định dạng!")
    private String sdtNguoiNhan;

    @NotBlank(message = "Địa chỉ cụ thể không được để trống!")
    @Size(min = 5, max = 255, message = "Địa chỉ cụ thể phải từ 5 đến 255 ký tự!")
    private String diaChiCuThe;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống!")
    @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự!")
    private String tinhThanh;

    @NotBlank(message = "Quốc gia không được để trống!")
    @Size(max = 100, message = "Quốc gia không được vượt quá 100 ký tự!")
    private String quocGia;

    private Double latitude;
    private Double longitude;
    private boolean defaultAddress;
}
