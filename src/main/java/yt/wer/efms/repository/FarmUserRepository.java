package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.FarmUser;
import yt.wer.efms.model.FarmUserId;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmUserRepository extends JpaRepository<FarmUser, FarmUserId> {
    Optional<FarmUser> findByFarmIdAndUserId(Long farmId, Long userId);
    Optional<FarmUser> findByFarmIdAndUserUsername(Long farmId, String username);
    List<FarmUser> findByUserId(Long userId);
    List<FarmUser> findByUserUsername(String username);
    List<FarmUser> findByFarmId(Long farmId);

    @Query("SELECT fu FROM FarmUser fu JOIN FETCH fu.farm f WHERE fu.id.userId = :userId AND f.deletedAt IS NULL")
    List<FarmUser> findByUserIdWithFarm(@Param("userId") Long userId);
}
