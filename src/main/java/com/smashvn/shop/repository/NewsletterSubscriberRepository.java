package com.smashvn.shop.repository;

import com.smashvn.shop.entity.NewsletterSubscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsletterSubscriberRepository extends JpaRepository<NewsletterSubscriber, Integer> {
    Optional<NewsletterSubscriber> findByEmail(String email);
    Optional<NewsletterSubscriber> findByTokenHuy(String tokenHuy);
    List<NewsletterSubscriber> findByTrangThai(String trangThai);
}
