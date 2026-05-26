package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Unit;

import java.util.List;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
    List<Unit> findByFarmIsNullOrderByIdAsc();
    List<Unit> findByFarmIsNullOrFarmIdOrderByIdAsc(Long farmId);
    boolean existsByValueAndFarmIsNull(String value);
}
