package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.CultureCode;

import java.util.Optional;

@Repository
public interface CultureCodeRepository extends JpaRepository<CultureCode, Long> {
    Optional<CultureCode> findByCode(String code);
}
