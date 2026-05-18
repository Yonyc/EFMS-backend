package yt.wer.efms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ParcelOperation;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ParcelOperationRepository extends JpaRepository<ParcelOperation, Long> {

	// Active operations only (for normal usage)
	@Query("SELECT DISTINCT po FROM ParcelOperation po JOIN po.parcels p WHERE p.id = :parcelId AND po.deletedAt IS NULL ORDER BY po.date DESC")
	List<ParcelOperation> findDistinctByParcelsIdOrderByDateDesc(@Param("parcelId") Long parcelId);

	@Query("SELECT DISTINCT po FROM ParcelOperation po JOIN po.parcels p WHERE p.id = :parcelId AND po.deletedAt IS NULL ORDER BY po.date DESC")
	Page<ParcelOperation> findDistinctByParcelsIdOrderByDateDesc(@Param("parcelId") Long parcelId, Pageable pageable);

	// Operations deleted at exact cascade timestamp (for selective restore)
	@Query("SELECT po FROM ParcelOperation po JOIN po.parcels p WHERE p.farm.id = :farmId AND po.deletedAt = :deletedAt")
	List<ParcelOperation> findByFarmIdAndDeletedAt(@Param("farmId") Long farmId, @Param("deletedAt") LocalDateTime deletedAt);
}
