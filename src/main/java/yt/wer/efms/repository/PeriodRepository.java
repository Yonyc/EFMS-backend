package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Period;

import java.util.List;

@Repository
public interface PeriodRepository extends JpaRepository<Period, Long> {
    List<Period> findByFarmId(Long farmId);
}
