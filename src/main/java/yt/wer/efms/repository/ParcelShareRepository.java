package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ParcelShare;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelShareRepository extends JpaRepository<ParcelShare, Long> {
    Optional<ParcelShare> findByParcelIdAndUserUsername(Long parcelId, String username);
    List<ParcelShare> findByParcelId(Long parcelId);
    List<ParcelShare> findByUserUsername(String username);
    List<ParcelShare> findByUserUsernameAndParcelFarmId(String username, Long farmId);
    void deleteByParcelIdAndUserId(Long parcelId, Long userId);
}
