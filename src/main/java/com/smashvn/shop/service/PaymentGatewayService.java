package com.smashvn.shop.service;

import com.smashvn.shop.dto.SepayIpnRequest;
import java.util.Map;

public interface PaymentGatewayService {
    Map<String, Object> handleIpn(SepayIpnRequest request, String rawPayload) throws Exception;
}
