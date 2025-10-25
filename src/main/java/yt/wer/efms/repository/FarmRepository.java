package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Farm;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {
}
