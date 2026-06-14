package com.smashvn.shop.controller.payment;

import com.smashvn.shop.dto.payment.ZaloPayCallbackDTO;
import com.smashvn.shop.dto.payment.ZaloPayCreateOrderRequestDTO;
import com.smashvn.shop.dto.payment.ZaloPayResponseDTO;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.payment.ZaloPayService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment/zalopay")
@RequiredArgsConstructor
@Slf4j
public class ZaloPayController {

    private final ZaloPayService zaloPayService;
    private final HoaDonRepository hoaDonRepository;

    @PostMapping("/create")
    public ResponseEntity<ZaloPayResponseDTO> createOrder(@RequestBody ZaloPayCreateOrderRequestDTO req, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<HoaDon> orderOpt = hoaDonRepository.findById(req.getOrderId());
        if (!orderOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        HoaDon order = orderOpt.get();
        String role = (String) session.getAttribute("vaiTro");
        boolean isAdminOrStaff = "QL".equals(role) || "NV".equals(role);
        boolean isOwner = order.getKhachHang() != null && order.getKhachHang().getTaiKhoan() != null
                && order.getKhachHang().getTaiKhoan().getId().equals(idNguoiDung);

        if (!isOwner && !isAdminOrStaff) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if ("DA_HUY".equals(order.getTrangThaiDonHang()) || "CANCELLED".equals(order.getPaymentStatus())) {
            log.warn("ZaloPay: Cannot create payment link for cancelled order #{}", req.getOrderId());
            return ResponseEntity.badRequest().body(ZaloPayResponseDTO.builder().status("FAILED").build());
        }

        if ("PAID".equals(order.getPaymentStatus())) {
            log.warn("ZaloPay: Cannot create payment link for already paid order #{}", req.getOrderId());
            return ResponseEntity.badRequest().body(ZaloPayResponseDTO.builder().status("FAILED").build());
        }

        try {
            ZaloPayResponseDTO resp = zaloPayService.createOrder(req.getOrderId());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("ZaloPay Controller: Error creating payment order: ", e);
            return ResponseEntity.badRequest().body(ZaloPayResponseDTO.builder()
                    .status("FAILED")
                    .build());
        }
    }

    @PostMapping("/callback")
    public ResponseEntity<Map<String, Object>> callback(@RequestBody ZaloPayCallbackDTO req) {
        try {
            log.info("ZaloPay Controller: Received callback webhook request");
            Map<String, Object> resp = zaloPayService.handleCallback(req);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("ZaloPay Controller: Error handling callback: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/query/{appTransId}")
    public ResponseEntity<ZaloPayResponseDTO> queryTransaction(@PathVariable("appTransId") String appTransId, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<HoaDon> orderOpt = hoaDonRepository.findByAppTransId(appTransId);
        if (!orderOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        HoaDon order = orderOpt.get();
        String role = (String) session.getAttribute("vaiTro");
        boolean isAdminOrStaff = "QL".equals(role) || "NV".equals(role);
        boolean isOwner = order.getKhachHang() != null && order.getKhachHang().getTaiKhoan() != null
                && order.getKhachHang().getTaiKhoan().getId().equals(idNguoiDung);

        if (!isOwner && !isAdminOrStaff) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            ZaloPayResponseDTO resp = zaloPayService.queryTransaction(appTransId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("ZaloPay Controller: Error querying transaction: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/cancel/{appTransId}")
    public ResponseEntity<Void> cancelTransaction(@PathVariable("appTransId") String appTransId, HttpSession session) {
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<HoaDon> orderOpt = hoaDonRepository.findByAppTransId(appTransId);
        if (!orderOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        HoaDon order = orderOpt.get();
        String role = (String) session.getAttribute("vaiTro");
        boolean isAdminOrStaff = "QL".equals(role) || "NV".equals(role);
        boolean isOwner = order.getKhachHang() != null && order.getKhachHang().getTaiKhoan() != null
                && order.getKhachHang().getTaiKhoan().getId().equals(idNguoiDung);

        if (!isOwner && !isAdminOrStaff) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!"PENDING".equals(order.getPaymentStatus())) {
            log.warn("ZaloPay: Cannot cancel payment since it is not in PENDING state. Order #{}", order.getId());
            return ResponseEntity.badRequest().build();
        }

        try {
            zaloPayService.cancelTransaction(appTransId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("ZaloPay Controller: Error cancelling transaction: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
