package com.anshul.devtoolkit.repository;

import com.anshul.devtoolkit.entity.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    List<RequestHistory> findTop50ByOrderByCreatedAtDesc();
}
