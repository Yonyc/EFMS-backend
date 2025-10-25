package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
