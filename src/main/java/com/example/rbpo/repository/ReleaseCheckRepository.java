package com.example.rbpo.repository;

import com.example.rbpo.entity.ReleaseCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReleaseCheckRepository
        extends JpaRepository<ReleaseCheck, Long> {

    boolean existsByRelease_IdAndCheckName(
            Long releaseId,
            String checkName
    );

    List<ReleaseCheck> findByRelease_Id(Long releaseId);
}