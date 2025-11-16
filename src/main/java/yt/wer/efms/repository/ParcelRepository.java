package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Parcel;
import java.util.List;

@Repository
public interface ParcelRepository extends JpaRepository<Parcel, Long> {

	// find parcels by farm id
	List<Parcel> findByFarmId(Long farmId);
}
