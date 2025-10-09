package utescore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import utescore.entity.Customer;
import utescore.repository.CustomerRepository;

import java.util.Optional;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    public Page<Customer> findAll(Pageable pageable) {
        return customerRepository.findAll(pageable);
    }

    // Tìm kiếm với từ khóa
    public Page<Customer> search(String searchTerm, Pageable pageable) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll(pageable);
        }
        return customerRepository.findBySearchTerm(searchTerm.trim(), pageable);
    }

    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> findByPhoneNumber(String phoneNumber) {
        return customerRepository.findByPhoneNumber(phoneNumber);
    }

    public Customer save(Customer customer) {
        // Validate số điện thoại không trùng lặp
        if (customer.getPhoneNumber() != null && !customer.getPhoneNumber().trim().isEmpty()) {
            if (customer.getId() == null) {
                // Tạo mới - kiểm tra số điện thoại đã tồn tại
                Optional<Customer> existing = customerRepository.findByPhoneNumber(customer.getPhoneNumber());
                if (existing.isPresent()) {
                    throw new RuntimeException("Số điện thoại đã được sử dụng bởi khách hàng khác");
                }
            } else {
                // Cập nhật - kiểm tra số điện thoại trùng với khách hàng khác
                Optional<Customer> existing = customerRepository.findByPhoneNumberAndIdNot(customer.getPhoneNumber(), customer.getId());
                if (existing.isPresent()) {
                    throw new RuntimeException("Số điện thoại đã được sử dụng bởi khách hàng khác");
                }
            }
        }
        
        return customerRepository.save(customer);
    }

    public void deleteById(Long id) {
        customerRepository.deleteById(id);
    }

    public boolean existsById(Long id) {
        return customerRepository.existsById(id);
    }

    public long count() {
        return customerRepository.count();
    }
}