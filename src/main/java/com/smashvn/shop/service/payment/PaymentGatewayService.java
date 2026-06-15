package com.smashvn.shop.service.payment;

import com.smashvn.shop.dto.payment.SepayIpnRequest;
import java.util.Map;

public interface PaymentGatewayService {
    Map<String, Object> handleIpn(SepayIpnRequest request, String rawPayload) throws Exception;
}
