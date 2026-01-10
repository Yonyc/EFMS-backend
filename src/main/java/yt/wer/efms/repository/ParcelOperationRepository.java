package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.ParcelOperation;

import java.util.List;

@Repository
public interface ParcelOperationRepository extends JpaRepository<ParcelOperation, Long> {
	List<ParcelOperation> findDistinctByParcelsIdOrderByDateDesc(Long parcelId);
}
