package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.ChatConversation;
import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Integer> {
    Optional<ChatConversation> findFirstByKhachHangIdAndTrangThaiOrderByNgayCapNhatDesc(Integer khachHangId, String trangThai);
    List<ChatConversation> findByKhachHangIdOrderByNgayCapNhatDesc(Integer khachHangId);
}
