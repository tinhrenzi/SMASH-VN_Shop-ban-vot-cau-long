package com.smashvn.shop.service;

import com.smashvn.shop.dto.ZaloPayResponseDTO;
import com.smashvn.shop.dto.ZaloPayCallbackDTO;
import java.util.Map;

public interface ZaloPayService {
    ZaloPayResponseDTO createOrder(Integer orderId) throws Exception;
    Map<String, Object> handleCallback(ZaloPayCallbackDTO callbackReq) throws Exception;
    ZaloPayResponseDTO queryTransaction(String appTransId) throws Exception;
    void cancelTransaction(String appTransId) throws Exception;
}
