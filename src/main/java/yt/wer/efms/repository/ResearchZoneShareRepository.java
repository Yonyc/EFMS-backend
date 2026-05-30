package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Farm;
import yt.wer.efms.model.ResearchZoneShare;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchZoneShareRepository extends JpaRepository<ResearchZoneShare, Long> {
    List<ResearchZoneShare> findByFarmId(Long farmId);

    List<ResearchZoneShare> findByFarmIdAndUserUsername(Long farmId, String username);

    List<ResearchZoneShare> findByUserUsername(String username);

    Optional<ResearchZoneShare> findByShareToken(String shareToken);

    @Query("SELECT DISTINCT rz.farm FROM ResearchZoneShare rz WHERE rz.user.username = :username AND rz.farm.deletedAt IS NULL AND (rz.shareStartAt IS NULL OR rz.shareStartAt <= :now) AND (rz.shareEndAt IS NULL OR rz.shareEndAt >= :now)")
    List<Farm> findActiveFarmsByUserUsername(@Param("username") String username, @Param("now") LocalDateTime now);
}
