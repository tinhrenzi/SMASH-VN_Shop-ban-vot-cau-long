package com.smashvn.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.config.SepayConfig;
import com.smashvn.shop.dto.SepayIpnRequest;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PaymentStatus;
import com.smashvn.shop.exception.InvalidPaymentException;
import com.smashvn.shop.exception.OrderNotFoundException;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.service.PaymentGatewayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class SepayIpnController {

    private final SepayConfig sepayConfig;
    private final PaymentGatewayService paymentGatewayService;
    private final HoaDonRepository hoaDonRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/api/payment/sepay/ipn")
    public ResponseEntity<Map<String, Object>> handleSepayIpn(
            @RequestBody String rawPayload,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        log.info("SePay IPN: Received callback webhook request");

        try {
            // 1. Verify Request IP Whitelist
            if (sepayConfig.isIpVerification()) {
                String clientIp = getClientIp(request);
                if (!isValidIp(clientIp)) {
                    log.warn("SePay IPN: Request rejected. Unauthorized IP: {}", clientIp);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Unauthorized IP address."));
                }
            }

            // 2. Verify Authorization Header (Apikey ipnSecret)
            String expectedAuth = "Apikey " + sepayConfig.getIpnSecret();
            if (authHeader == null || !authHeader.trim().equals(expectedAuth)) {
                log.warn("SePay IPN: Request rejected. Invalid Authorization Header.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("Invalid API credentials."));
            }

            // Deserialize payload
            SepayIpnRequest ipnRequest = objectMapper.readValue(rawPayload, SepayIpnRequest.class);

            // 3. Process IPN payload (handles duplicate transactions internally and returns proper message)
            Map<String, Object> result;
            try {
                result = paymentGatewayService.handleIpn(ipnRequest, rawPayload);
            } catch (DataIntegrityViolationException e) {
                log.info("SePay IPN: Database unique constraint triggered on concurrent duplicate.");
                return ResponseEntity.ok(createSuccessResponse("Already processed", ipnRequest.getTransactionData().getTransactionId()));
            }

            return ResponseEntity.ok(result);

        } catch (InvalidPaymentException e) {
            log.error("SePay IPN Validation Error: {}", e.getMessage());
            return ResponseEntity.ok(createSuccessResponse(e.getMessage()));
        } catch (OrderNotFoundException e) {
            log.error("SePay IPN Order Error: {}", e.getMessage());
            return ResponseEntity.ok(createSuccessResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("SePay IPN Unhandled Exception: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorResponse("Internal server error."));
        }
    }

    @GetMapping("/api/payment/sepay/query/{maDonHang}")
    public ResponseEntity<Map<String, Object>> queryTransaction(
            @PathVariable("maDonHang") String maDonHang,
            HttpSession session) {

        // 1. Ownership: Validate session customer ID exists
        Integer idNguoiDung = (Integer) session.getAttribute("idNguoiDung");
        if (idNguoiDung == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(createErrorResponse("Unauthorized session. Please login."));
        }

        // 2. Retrieve order
        Optional<HoaDon> orderOpt = hoaDonRepository.findByMaDonHang(maDonHang);
        if (!orderOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(createErrorResponse("Order not found."));
        }

        HoaDon order = orderOpt.get();

        // 3. Ownership: Validate order owner matches session customer
        if (order.getKhachHang() == null || order.getKhachHang().getTaiKhoan() == null ||
                !order.getKhachHang().getTaiKhoan().getId().equals(idNguoiDung)) {
            log.warn("SePay Query: Ownership validation failed for user #{} trying to query order code {}", idNguoiDung, maDonHang);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(createErrorResponse("Access Denied."));
        }

        // 4. Return standardized query API response
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("orderCode", order.getMaDonHang());
        resp.put("paymentStatus", order.getPaymentStatus());
        resp.put("orderStatus", order.getTrangThaiDonHang());
        return ResponseEntity.ok(resp);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean isValidIp(String clientIp) {
        if (clientIp == null) {
            return false;
        }
        // Allow loopbacks in development
        if ("127.0.0.1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp) || "localhost".equals(clientIp)) {
            return true;
        }
        String allowedIpsStr = sepayConfig.getIpRanges();
        if (allowedIpsStr == null || allowedIpsStr.trim().isEmpty()) {
            return true;
        }
        String[] allowedIps = allowedIpsStr.split(",");
        for (String ip : allowedIps) {
            if (ip.trim().equals(clientIp)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        return resp;
    }

    private Map<String, Object> createSuccessResponse(String message, String transactionId) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("message", message);
        resp.put("transactionId", transactionId);
        return resp;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", false);
        resp.put("message", message);
        return resp;
    }
}
