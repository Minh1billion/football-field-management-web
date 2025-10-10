package utescore.repository;

import org.springframework.data.jpa.repository.JpaRepository;

// Lưu ý entity tên Service trùng với stereotype, nên import đầy đủ
public interface ServiceRepository extends JpaRepository<utescore.entity.Service, Long> {
}