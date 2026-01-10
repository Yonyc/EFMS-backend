package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Unit;

@Repository
public interface UnitRepository extends JpaRepository<Unit, Long> {
}
