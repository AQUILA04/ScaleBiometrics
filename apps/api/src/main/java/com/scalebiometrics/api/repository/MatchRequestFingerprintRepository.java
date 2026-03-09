package com.scalebiometrics.api.repository;

import com.scalebiometrics.api.entity.MatchRequestFingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MatchRequestFingerprintRepository extends JpaRepository<MatchRequestFingerprint, UUID> {
}
