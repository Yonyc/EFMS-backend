package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.OperationProduct;

import java.util.List;

@Repository
public interface OperationProductRepository extends JpaRepository<OperationProduct, Long> {
    List<OperationProduct> findByOperationId(Long operationId);

    void deleteByOperationId(Long operationId);
}
