package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Parcel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

	// Active parcels only
	List<Parcel> findByFarmIdAndDeletedAtIsNull(Long farmId);

	// All parcels for a farm (including deleted) – used for cascade operations
	List<Parcel> findByFarmId(Long farmId);

	// Parcels deleted at an exact cascade timestamp (for selective restore)
	List<Parcel> findByFarmIdAndDeletedAt(Long farmId, LocalDateTime deletedAt);

	Optional<Parcel> findByIdAndDeletedAtIsNull(Long id);

	Parcel findByCorrespondingPacId(Long importedParcelId);

	List<Parcel> findByCorrespondingPacImportRecordId(Long importRecordId);

	@Query(value = "SELECT DISTINCT p.* " +
			"FROM parcels p " +
			"LEFT JOIN parcels_parcel_operations ppo ON ppo.parcels_id = p.id " +
			"LEFT JOIN parcel_operations po ON po.id = ppo.parcel_operations_parcels " +
			"LEFT JOIN operation_products op ON op.operation = po.id " +
			"WHERE p.farm = :farmId " +
			"AND p.deleted_at IS NULL " +
			"AND (:periodFilter = false OR p.period IN (:periodIds)) " +
			"AND (:operationTypeFilter = false OR po.type IN (:operationTypeIds)) " +
			"AND (:toolFilter = false OR op.tool IN (:toolIds)) " +
			"AND (:productFilter = false OR op.product IN (:productIds)) " +
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
			"AND p.geodata IS NOT NULL " +
			"AND ST_Intersects(ST_SetSRID(p.geodata, 4326), ST_MakeEnvelope(CAST(:minLng AS double precision), CAST(:minLat AS double precision), CAST(:maxLng AS double precision), CAST(:maxLat AS double precision), 4326))",
		nativeQuery = true)
	List<Parcel> findByFarmIdWithinBounds(@Param("farmId") Long farmId,
								 @Param("minLng") Double minLng,
								 @Param("minLat") Double minLat,
								 @Param("maxLng") Double maxLng,
								 @Param("maxLat") Double maxLat);
}
