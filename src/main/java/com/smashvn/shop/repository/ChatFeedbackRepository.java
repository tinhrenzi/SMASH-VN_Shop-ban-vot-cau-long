package com.smashvn.shop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.smashvn.shop.entity.ChatFeedback;

public interface ChatFeedbackRepository extends JpaRepository<ChatFeedback, Integer> {
}
