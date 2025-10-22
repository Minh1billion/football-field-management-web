package utescore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import utescore.entity.Customer;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
	@Query("SELECT c FROM Customer c")
	List<Customer> findAll();
    // Tìm kiếm theo số điện thoại
    Optional<Customer> findByPhoneNumber(String phoneNumber);

    // Tìm kiếm tổng quát
    @Query("SELECT c FROM Customer c WHERE " +
            "LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.address) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Customer> findBySearchTerm(@Param("search") String search, Pageable pageable);

    // Kiểm tra số điện thoại đã tồn tại (trừ ID hiện tại)
    @Query("SELECT c FROM Customer c WHERE c.phoneNumber = :phoneNumber AND c.id != :id")
    Optional<Customer> findByPhoneNumberAndIdNot(@Param("phoneNumber") String phoneNumber, @Param("id") Long id);

    // Tìm id theo tên người dùng của tài khoản khách hàng
    Customer findByAccount_Username(String username);
}