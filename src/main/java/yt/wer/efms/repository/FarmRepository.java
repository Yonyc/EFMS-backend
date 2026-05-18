package yt.wer.efms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Farm;

import java.util.List;
import java.util.Optional;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {
    // Active farms only (for normal usage)
    List<Farm> findByOwnerUsernameAndDeletedAtIsNull(String username);
    List<Farm> findByIsPublicTrueAndDeletedAtIsNull();
    Page<Farm> findByIsPublicTrueAndDeletedAtIsNull(Pageable pageable);

    // All farms including deleted (for admin)
    List<Farm> findByOwnerUsername(String username);
    List<Farm> findByIsPublicTrue();

    Optional<Farm> findByIdAndDeletedAtIsNull(Long id);
}
