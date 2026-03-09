package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Parcel;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

	// find parcels by farm id
	List<Parcel> findByFarmId(Long farmId);

	Parcel findByCorrespondingPacId(Long importedParcelId);

	List<Parcel> findByCorrespondingPacImportRecordId(Long importRecordId);

	@Query(value = "SELECT DISTINCT p.* " +
			"FROM parcels p " +
			"LEFT JOIN parcels_parcel_operations ppo ON ppo.parcels_id = p.id " +
			"LEFT JOIN parcel_operations po ON po.id = ppo.parcel_operations_parcels " +
			"LEFT JOIN operation_products op ON op.operation = po.id " +
			"WHERE p.farm = :farmId " +
			"AND (CAST(:periodId AS bigint) IS NULL OR p.period = CAST(:periodId AS bigint)) " +
			"AND (CAST(:toolId AS bigint) IS NULL OR op.tool = CAST(:toolId AS bigint)) " +
			"AND (CAST(:productId AS bigint) IS NULL OR op.product = CAST(:productId AS bigint)) " +
			"AND (CAST(:startDate AS timestamp) IS NULL OR po.date >= CAST(:startDate AS timestamp)) " +
			"AND (CAST(:endDate AS timestamp) IS NULL OR po.date <= CAST(:endDate AS timestamp)) " +
			"AND (CAST(:polygonWkt AS text) IS NULL OR (p.geodata IS NOT NULL AND ST_Intersects(ST_SetSRID(p.geodata, 4326), ST_GeomFromText(CAST(:polygonWkt AS text), 4326)))) " +
			"AND (CAST(:minLng AS double precision) IS NULL OR (p.geodata IS NOT NULL AND ST_Intersects(ST_SetSRID(p.geodata, 4326), ST_MakeEnvelope(CAST(:minLng AS double precision), CAST(:minLat AS double precision), CAST(:maxLng AS double precision), CAST(:maxLat AS double precision), 4326))))",
		nativeQuery = true)
	List<Parcel> searchParcels(@Param("farmId") Long farmId,
						  @Param("periodId") Long periodId,
						  @Param("toolId") Long toolId,
						  @Param("productId") Long productId,
						  @Param("startDate") LocalDateTime startDate,
						  @Param("endDate") LocalDateTime endDate,
						  @Param("polygonWkt") String polygonWkt,
						  @Param("minLng") Double minLng,
						  @Param("minLat") Double minLat,
						  @Param("maxLng") Double maxLng,
						  @Param("maxLat") Double maxLat);
}
