package com.example.rbpo.repository;

import com.example.rbpo.entity.DevelopmentTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevelopmentTaskRepository
        extends JpaRepository<DevelopmentTask, Long> {

    boolean existsByProduct_Id(Long productId);

    List<DevelopmentTask> findByProduct_IdAndVersion(
            Long productId,
            String version
    );
}