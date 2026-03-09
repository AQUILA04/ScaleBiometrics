package com.scalebiometrics.api.repository;

import com.scalebiometrics.core.domain.Fingerprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FingerprintRepository extends JpaRepository<Fingerprint, UUID> {
    // Additional query methods can be defined here
}
