package com.smashvn.shop.service.payment;

import com.smashvn.shop.dto.payment.ZaloPayResponseDTO;
import com.smashvn.shop.dto.payment.ZaloPayCallbackDTO;
import java.util.Map;

public interface ZaloPayService {
    ZaloPayResponseDTO createOrder(Integer orderId) throws Exception;
    Map<String, Object> handleCallback(ZaloPayCallbackDTO callbackReq) throws Exception;
    ZaloPayResponseDTO queryTransaction(String appTransId) throws Exception;
    void cancelTransaction(String appTransId) throws Exception;
}
