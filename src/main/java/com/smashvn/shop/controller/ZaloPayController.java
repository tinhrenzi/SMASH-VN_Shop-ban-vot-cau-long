package com.smashvn.shop.controller;

import com.smashvn.shop.dto.ZaloPayCallbackDTO;
import com.smashvn.shop.dto.ZaloPayCreateOrderRequestDTO;
import com.smashvn.shop.dto.ZaloPayResponseDTO;
import com.smashvn.shop.service.ZaloPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/zalopay")
@RequiredArgsConstructor
@Slf4j
public class ZaloPayController {

    private final ZaloPayService zaloPayService;

    @PostMapping("/create")
    public ResponseEntity<ZaloPayResponseDTO> createOrder(@RequestBody ZaloPayCreateOrderRequestDTO req) {
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
    public ResponseEntity<ZaloPayResponseDTO> queryTransaction(@PathVariable("appTransId") String appTransId) {
        try {
            ZaloPayResponseDTO resp = zaloPayService.queryTransaction(appTransId);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            log.error("ZaloPay Controller: Error querying transaction: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/cancel/{appTransId}")
    public ResponseEntity<Void> cancelTransaction(@PathVariable("appTransId") String appTransId) {
        try {
            zaloPayService.cancelTransaction(appTransId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("ZaloPay Controller: Error cancelling transaction: ", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
