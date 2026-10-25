package com.example.rbpo.repository;

import com.example.rbpo.entity.Release;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.rbpo.repository.ReleaseRepository;
import java.util.List;

public interface ReleaseRepository
        extends JpaRepository<Release, Long> {

    boolean existsByProduct_Id(Long productId);

    boolean existsByProduct_IdAndNumber(
            Long productId,
            String number
    );

    boolean existsByProduct_IdAndNumberAndStatusIn(
            Long productId,
            String number,
            List<String> statuses
    );
}