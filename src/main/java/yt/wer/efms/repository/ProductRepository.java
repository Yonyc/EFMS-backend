package yt.wer.efms.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	List<Product> findByFarmId(Long farmId);
	Optional<Product> findByFarmIdAndSourceGuid(Long farmId, String sourceGuid);
	Page<Product> findByFarmId(Long farmId, Pageable pageable);

	List<Product> findByOfficialTrueAndOfficialCurrentTrue();
	Page<Product> findByOfficialTrueAndOfficialCurrentTrue(Pageable pageable);

	Optional<Product> findByOfficialAuthNumberAndOfficialCurrentTrue(String officialAuthNumber);

	@Query("SELECT p FROM Product p " +
			"WHERE p.official = true AND p.officialCurrent = true " +
			"AND (cast(:query as String) IS NULL OR cast(:query as String) = '' " +
			"  OR lower(p.name) LIKE lower(concat('%', cast(:query as String), '%')) " +
			"  OR lower(p.officialAuthNumber) LIKE lower(concat('%', cast(:query as String), '%')) " +
			"  OR lower(coalesce(p.officialActiveSubstances, '')) LIKE lower(concat('%', cast(:query as String), '%')) " +
			"  OR lower(coalesce(p.officialHolderName, '')) LIKE lower(concat('%', cast(:query as String), '%'))) " +
			"AND (cast(:decisionCode as String) IS NULL OR p.officialDecisionCode = cast(:decisionCode as String)) " +
			"AND (cast(:userGroupCode as String) IS NULL OR p.officialUserGroupCode = cast(:userGroupCode as String)) " +
			"AND (cast(:productTypeCode as String) IS NULL " +
			"  OR lower(coalesce(p.officialProductTypeCodes, '')) LIKE lower(concat('%', cast(:productTypeCode as String), '%'))) " +
			"ORDER BY p.name ASC")
	Page<Product> searchOfficialProducts(
			@Param("query") String query,
			@Param("decisionCode") String decisionCode,
			@Param("userGroupCode") String userGroupCode,
			@Param("productTypeCode") String productTypeCode,
			Pageable pageable);

	@Query("SELECT DISTINCT p.officialDecisionCode, p.officialDecisionCodeFr FROM Product p " +
			"WHERE p.official = true AND p.officialCurrent = true AND p.officialDecisionCode IS NOT NULL")
	List<Object[]> findDistinctDecisionCodes();

	@Query("SELECT DISTINCT p.officialUserGroupCode, p.officialUserGroupFr FROM Product p " +
			"WHERE p.official = true AND p.officialCurrent = true AND p.officialUserGroupCode IS NOT NULL")
	List<Object[]> findDistinctUserGroupCodes();

	@Query("SELECT DISTINCT p.officialProductTypeCodes, p.officialProductTypeFr FROM Product p " +
			"WHERE p.official = true AND p.officialCurrent = true AND p.officialProductTypeCodes IS NOT NULL")
	List<Object[]> findDistinctProductTypePairs();

	@Query("SELECT p FROM Product p " +
			"WHERE (p.farm.id = :farmId OR (p.official = true AND p.officialCurrent = true)) " +
			"AND (cast(:query as String) IS NULL OR cast(:query as String) = '' " +
			"    OR lower(p.name) LIKE lower(concat('%', cast(:query as String), '%')) " +
			"    OR lower(coalesce(p.officialAuthNumber, '')) LIKE lower(concat('%', cast(:query as String), '%'))) " +
			"ORDER BY CASE WHEN p.farm IS NOT NULL THEN 0 ELSE 1 END ASC, p.name ASC")
	Page<Product> searchFarmAndOfficialProducts(
			@Param("farmId") Long farmId,
			@Param("query") String query,
			Pageable pageable);

	@Query("SELECT p FROM Product p WHERE p.farm.id = :farmId " +
			"AND (:query IS NULL OR :query = '' OR lower(p.name) LIKE lower(concat('%', :query, '%'))) " +
			"AND (:productTypeId IS NULL OR p.productType.id = :productTypeId) " +
			"AND (:unitId IS NULL OR p.unit.id = :unitId) " +
			"AND (:defaultOpTypeId IS NULL OR p.defaultOperationType.id = :defaultOpTypeId)")
	Page<Product> searchByFarm(@Param("farmId") Long farmId,
							   @Param("query") String query,
							   @Param("productTypeId") Long productTypeId,
							   @Param("unitId") Long unitId,
							   @Param("defaultOpTypeId") Long defaultOpTypeId,
							   Pageable pageable);

	List<Product> findByImportRecordId(Long importRecordId);
	void deleteByImportRecordId(Long importRecordId);
	List<Product> findBySourceFileId(Long sourceFileId);
	void deleteBySourceFileId(Long sourceFileId);
	Optional<Product> findByImportRecordIdAndSourceGuid(Long importRecordId, String sourceGuid);
}
