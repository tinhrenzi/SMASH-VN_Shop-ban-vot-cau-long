package com.smashvn.shop.repository;

import com.smashvn.shop.entity.CommentModerationKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentModerationKeywordRepository extends JpaRepository<CommentModerationKeyword, Integer> {
    List<CommentModerationKeyword> findAllByActiveTrue();
}
