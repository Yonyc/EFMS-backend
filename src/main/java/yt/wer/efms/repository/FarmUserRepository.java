package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.FarmUser;
import yt.wer.efms.model.FarmUserId;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmUserRepository extends JpaRepository<FarmUser, FarmUserId> {
    Optional<FarmUser> findByFarmIdAndUserUsername(Long farmId, String username);
    List<FarmUser> findByUserUsername(String username);
    List<FarmUser> findByFarmId(Long farmId);
}
