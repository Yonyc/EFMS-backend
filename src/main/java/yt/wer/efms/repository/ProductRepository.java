package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}
