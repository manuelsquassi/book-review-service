package com.squassi.bookreview.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.squassi.bookreview.entity.ReviewEntity;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
}
