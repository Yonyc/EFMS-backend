package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ResearchZoneShare;
import yt.wer.efms.model.ResearchZoneShareClaim;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResearchZoneShareClaimRepository extends JpaRepository<ResearchZoneShareClaim, Long> {
    boolean existsByShareIdAndUserId(Long shareId, Long userId);

    @Query("SELECT COUNT(DISTINCT c.user.id) FROM ResearchZoneShareClaim c WHERE c.share.id = :shareId")
    long countDistinctUsersByShareId(@Param("shareId") Long shareId);

    @Query("SELECT c.share FROM ResearchZoneShareClaim c WHERE c.share.farm.id = :farmId AND c.user.username = :username")
    List<ResearchZoneShare> findSharesByFarmIdAndUsername(@Param("farmId") Long farmId, @Param("username") String username);

    @Query("SELECT c.share.farm FROM ResearchZoneShareClaim c WHERE c.user.username = :username")
    List<yt.wer.efms.model.Farm> findClaimedFarmsByUsername(@Param("username") String username);

    @Query("SELECT DISTINCT c.user.username FROM ResearchZoneShareClaim c WHERE c.share.id = :shareId")
    List<String> findClaimedUsernamesByShareId(@Param("shareId") Long shareId);

    Optional<ResearchZoneShareClaim> findByShareIdAndUserId(Long shareId, Long userId);
}
