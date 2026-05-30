package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ParcelPeriod;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelPeriodRepository extends JpaRepository<ParcelPeriod, Long> {
    Optional<ParcelPeriod> findByParcelIdAndPeriodId(Long parcelId, Long periodId);
    List<ParcelPeriod> findByParcelId(Long parcelId);
    List<ParcelPeriod> findByPeriodId(Long periodId);
    void deleteByParcelId(Long parcelId);
}
