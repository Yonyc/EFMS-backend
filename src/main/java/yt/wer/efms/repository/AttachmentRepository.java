package yt.wer.efms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import yt.wer.efms.model.Attachment;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByOperationId(Long operationId);
    List<Attachment> findByProductId(Long productId);
    List<Attachment> findByToolId(Long toolId);
}
