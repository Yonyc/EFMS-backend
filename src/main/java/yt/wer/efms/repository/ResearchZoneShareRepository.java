package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ResearchZoneShare;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchZoneShareRepository extends JpaRepository<ResearchZoneShare, Long> {
    List<ResearchZoneShare> findByFarmId(Long farmId);

    List<ResearchZoneShare> findByFarmIdAndUserUsername(Long farmId, String username);

    List<ResearchZoneShare> findByUserUsername(String username);

    Optional<ResearchZoneShare> findByShareToken(String shareToken);
}
