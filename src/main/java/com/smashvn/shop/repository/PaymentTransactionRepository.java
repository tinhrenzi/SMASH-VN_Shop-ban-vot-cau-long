package com.smashvn.shop.repository;

import com.smashvn.shop.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {

    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    List<PaymentTransaction> findByOrder_Id(Integer orderId);

    @Query("SELECT pt FROM PaymentTransaction pt LEFT JOIN pt.order o WHERE " +
           "(:orderCode IS NULL OR o.maDonHang LIKE %:orderCode%) AND " +
           "(:transactionId IS NULL OR pt.transactionId LIKE %:transactionId%) AND " +
           "(:status IS NULL OR pt.status = :status) AND " +
           "(:startDate IS NULL OR pt.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR pt.createdAt <= :endDate) " +
           "ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> filterTransactions(
        @Param("orderCode") String orderCode,
        @Param("transactionId") String transactionId,
        @Param("status") String status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
