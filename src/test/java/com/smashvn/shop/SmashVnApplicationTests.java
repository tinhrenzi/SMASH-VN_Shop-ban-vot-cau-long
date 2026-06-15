package com.smashvn.shop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import com.smashvn.shop.repository.HoaDonRepository;
import com.smashvn.shop.repository.PaymentTransactionRepository;
import com.smashvn.shop.entity.HoaDon;
import com.smashvn.shop.entity.PaymentTransaction;
import java.util.List;

@SpringBootTest
class SmashVnApplicationTests {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Test
    @org.springframework.transaction.annotation.Transactional
    void contextLoads() {
        System.out.println("=== REAL DB HOA DON ORDERS ===");
        List<HoaDon> orders = hoaDonRepository.findAll();
        for (HoaDon hd : orders) {
            if (hd.getMaDonHang() != null && hd.getMaDonHang().startsWith("DHSVN")) {
                System.out.println("Order: " + hd.getMaDonHang() 
                    + " | PaymentStatus: " + hd.getPaymentStatus()
                    + " | OrderStatus: " + hd.getTrangThaiDonHang()
                    + " | TrangThaiThanhToan: " + hd.getTrangThaiThanhToan()
                    + " | GatewayResponse: " + hd.getGatewayResponse()
                    + " | NgayTao: " + hd.getNgayTao());
            }
        }

        System.out.println("=== REAL DB PAYMENT TRANSACTIONS ===");
        List<PaymentTransaction> txs = paymentTransactionRepository.findAll();
        for (PaymentTransaction tx : txs) {
            System.out.println("TransactionId: " + tx.getTransactionId()
                + " | OrderCode: " + (tx.getOrder() != null ? tx.getOrder().getMaDonHang() : "null")
                + " | Amount: " + tx.getAmount()
                + " | Status: " + tx.getStatus()
                + " | CreatedAt: " + tx.getCreatedAt());
        }
    }

}

