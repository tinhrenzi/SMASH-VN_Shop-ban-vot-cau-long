package com.smashvn.shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.dto.SepayIpnRequest;
import com.smashvn.shop.dto.SepayTransactionDto;
import com.smashvn.shop.service.SepayGatewayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SepaySimulationController {

    private final HoaDonRepository hoaDonRepository;
    private final SepayGatewayService sepayGatewayService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/payment/sepay/simulate")
    public String showSimulationPage(@RequestParam("maDonHang") String maDonHang, Model model) {
        Optional<HoaDon> orderOpt = hoaDonRepository.findByMaDonHang(maDonHang);
        if (!orderOpt.isPresent()) {
            model.addAttribute("error", "Không tìm thấy đơn hàng: " + maDonHang);
            return "sepay-simulate";
        }

        HoaDon hd = orderOpt.get();
        model.addAttribute("order", hd);
        return "sepay-simulate";
    }

    @PostMapping("/payment/sepay/simulate/success")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> simulateSuccess(
            @RequestParam("maDonHang") String maDonHang,
            @RequestParam("amount") BigDecimal amount) {

        log.info("Simulating successful SePay payment for order {}", maDonHang);

        try {
            // Construct simulated transaction DTO
            SepayTransactionDto tx = new SepayTransactionDto();
            String transactionId = "SIM_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            tx.setTransactionId(transactionId);
            tx.setTransferAmount(amount);
            tx.setAccountNumber("******7890");
            tx.setContent("Thanh toan don hang " + maDonHang);
            tx.setCode(maDonHang);
            tx.setReferenceCode("-");
            tx.setGateway("Vietcombank");
            tx.setStatus("success");
            tx.setTransactionDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            // Wrap in IPN Request
            SepayIpnRequest ipnRequest = new SepayIpnRequest();
            ipnRequest.setTransaction(tx);
            ipnRequest.setTransactionId(transactionId);
            ipnRequest.setTransferAmount(amount);
            ipnRequest.setContent("Thanh toan don hang " + maDonHang);
            ipnRequest.setCode(maDonHang);
            ipnRequest.setStatus("success");

            String rawJson = objectMapper.writeValueAsString(ipnRequest);

            // Directly call handleIpn
            Map<String, Object> result = sepayGatewayService.handleIpn(ipnRequest, rawJson);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Simulated successfully!");
            response.put("result", result);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Simulation failed:", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
