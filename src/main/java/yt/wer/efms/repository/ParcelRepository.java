package yt.wer.efms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Parcel;
import yt.wer.efms.model.ParcelStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

	List<Parcel> findByFarmIdAndStatusAndDeletedAtIsNull(Long farmId, ParcelStatus status);

	List<Parcel> findByFarmIdAndDeletedAtIsNull(Long farmId);

	List<Parcel> findByFarmIdAndParentParcelIdInAndDeletedAtIsNull(Long farmId, java.util.Collection<Long> parentIds);

	// All parcels for a farm – used for cascade operations
	List<Parcel> findByFarmId(Long farmId);

	// Parcels deleted at an exact cascade timestamp
	List<Parcel> findByFarmIdAndDeletedAt(Long farmId, LocalDateTime deletedAt);

	Optional<Parcel> findByIdAndDeletedAtIsNull(Long id);

	Optional<Parcel> findByFarmIdAndSourceGuidAndDeletedAtIsNull(Long farmId, String sourceGuid);


	List<Parcel> findByImportRecordIdAndStatus(Long importRecordId, ParcelStatus status);
	Page<Parcel> findByImportRecordIdAndStatus(Long importRecordId, ParcelStatus status, Pageable pageable);
	List<Parcel> findByImportRecordId(Long importRecordId);
	List<Parcel> findBySourceFileIdAndStatus(Long sourceFileId, ParcelStatus status);
	void deleteBySourceFileIdAndStatus(Long sourceFileId, ParcelStatus status);
	boolean existsByImportRecordIdAndSourceGuidAndStatus(Long importRecordId, String sourceGuid, ParcelStatus status);

	@Query(value = "SELECT DISTINCT p.* " +
			"FROM parcels p " +
			"LEFT JOIN parcels_parcel_operations ppo ON ppo.parcels_id = p.id " +
			"LEFT JOIN parcel_operations po ON po.id = ppo.parcel_operations_parcels " +
			"LEFT JOIN operation_products op ON op.operation = po.id " +
			"WHERE p.farm = :farmId " +
			"AND p.deleted_at IS NULL " +
			"AND p.status = 'LIVE' " +
			"AND (:periodFilter = false OR p.id IN (SELECT pp.parcel_id FROM parcel_periods pp WHERE pp.period_id IN (:periodIds))) " +
			"AND ( " +
			"  (:operationFiltersUnion = true AND ( " +
			"     :anyOperationFilter = false " +
			"     OR (:operationTypeFilter = true AND po.type IN (:operationTypeIds)) " +
			"     OR (:toolFilter = true AND op.tool IN (:toolIds)) " +
			"     OR (:productFilter = true AND op.product IN (:productIds)) " +
			"  )) " +
			"  OR (:operationFiltersUnion = false AND ( " +
			"     (:operationTypeFilter = false OR po.type IN (:operationTypeIds)) " +
			"     AND (:toolFilter = false OR op.tool IN (:toolIds)) " +
			"     AND (:productFilter = false OR op.product IN (:productIds)) " +
			"  )) " +
			") " +
			"AND (CAST(:startDate AS timestamp) IS NULL OR po.date >= CAST(:startDate AS timestamp)) " +
			"AND (CAST(:endDate AS timestamp) IS NULL OR po.date <= CAST(:endDate AS timestamp)) " +
			"AND (CAST(:polygonWkt AS text) IS NULL OR (p.geodata IS NOT NULL AND ST_Intersects(ST_SetSRID(p.geodata, 4326), ST_GeomFromText(CAST(:polygonWkt AS text), 4326)))) " +
			"AND (CAST(:minLng AS double precision) IS NULL OR (p.geodata IS NOT NULL AND ST_Intersects(ST_SetSRID(p.geodata, 4326), ST_MakeEnvelope(CAST(:minLng AS double precision), CAST(:minLat AS double precision), CAST(:maxLng AS double precision), CAST(:maxLat AS double precision), 4326))))",
		nativeQuery = true)
	List<Parcel> searchParcels(@Param("farmId") Long farmId,
							  @Param("periodFilter") boolean periodFilter,
							  @Param("periodIds") List<Long> periodIds,
						  @Param("operationTypeFilter") boolean operationTypeFilter,
						  @Param("operationTypeIds") List<Long> operationTypeIds,
							  @Param("toolFilter") boolean toolFilter,
							  @Param("toolIds") List<Long> toolIds,
							  @Param("productFilter") boolean productFilter,
							  @Param("productIds") List<Long> productIds,
							  @Param("anyOperationFilter") boolean anyOperationFilter,
							  @Param("operationFiltersUnion") boolean operationFiltersUnion,
							  @Param("startDate") LocalDateTime startDate,
							  @Param("endDate") LocalDateTime endDate,
							  @Param("polygonWkt") String polygonWkt,
							  @Param("minLng") Double minLng,
							  @Param("minLat") Double minLat,
							  @Param("maxLng") Double maxLng,
							  @Param("maxLat") Double maxLat);

	@Query(value = "SELECT p.* FROM parcels p " +
			"WHERE p.farm = :farmId " +
			"AND p.deleted_at IS NULL " +
			"AND p.status = 'LIVE' " +
			"AND p.geodata IS NOT NULL " +
			"AND ST_Intersects(ST_SetSRID(p.geodata, 4326), ST_MakeEnvelope(CAST(:minLng AS double precision), CAST(:minLat AS double precision), CAST(:maxLng AS double precision), CAST(:maxLat AS double precision), 4326)) " +
			"AND (:periodFilter = false " +
			"     OR EXISTS (SELECT 1 FROM parcel_periods pp WHERE pp.parcel_id = p.id AND pp.period_id IN (:periodIds)) " +
			"     OR NOT EXISTS (SELECT 1 FROM parcel_periods pp2 WHERE pp2.parcel_id = p.id))",
		nativeQuery = true)
	List<Parcel> findByFarmIdWithinBounds(@Param("farmId") Long farmId,
								 @Param("minLng") Double minLng,
								 @Param("minLat") Double minLat,
								 @Param("maxLng") Double maxLng,
								 @Param("maxLat") Double maxLat,
								 @Param("periodFilter") boolean periodFilter,
								 @Param("periodIds") List<Long> periodIds);
}
