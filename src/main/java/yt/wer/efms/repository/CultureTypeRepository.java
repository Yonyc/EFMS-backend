package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.CultureType;

import java.util.List;
import java.util.Optional;

@Repository
public interface CultureTypeRepository extends JpaRepository<CultureType, Long> {
    /** All global culture types. */
    List<CultureType> findByFarmIsNullOrderByCodeAsc();

    /** Farm-specific custom types. */
    List<CultureType> findByFarmIdOrderByCodeAsc(Long farmId);

    /** Global + farm-specific combined. */
    List<CultureType> findByFarmIsNullOrFarmIdOrderByCodeAsc(Long farmId);

    boolean existsByCodeAndFarmIsNull(String code);
    boolean existsByCodeAndFarmId(String code, Long farmId);

    Optional<CultureType> findByCodeAndFarmIsNull(String code);

    /** Resolve a culture type by code, preferring a farm-specific override. */
    Optional<CultureType> findFirstByCodeAndFarmId(String code, Long farmId);

    long countByFarmIsNull();
}
