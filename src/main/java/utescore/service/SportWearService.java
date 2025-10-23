package utescore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import utescore.entity.SportWear;
import utescore.repository.SportWearRepository;

import java.util.List;
import java.util.Optional;

@Service
public class SportWearService {

    @Autowired
    private SportWearRepository sportWearRepository;

    public Page<SportWear> findAll(Pageable pageable) {
        return sportWearRepository.findAll(pageable);
    }

    public Page<SportWear> findByNameContainingAndWearTypeAndSize(
            String name, SportWear.WearType wearType, SportWear.Size size, Pageable pageable) {
        return sportWearRepository.findByNameContainingAndWearTypeAndSize(name, wearType, size, pageable);
    }

    public List<SportWear> findAll() {
        return sportWearRepository.findAll();
    }

    public SportWear findById(Long id) {
        Optional<SportWear> sportWear = sportWearRepository.findById(id);
        return sportWear.orElse(null);
    }

    public SportWear save(SportWear sportWear) {
        return sportWearRepository.save(sportWear);
    }

    public SportWear update(SportWear sportWear) {
        return sportWearRepository.save(sportWear);
    }

    public void deleteById(Long id) {
        sportWearRepository.deleteById(id);
    }

    public List<SportWear> findAvailableForRent(Pageable pageable) {
        if (pageable == null) {
            // Lấy tất cả đồ khả dụng nếu pageable = null
            return sportWearRepository.findByIsAvailableForRentTrue();
        }
        return sportWearRepository.findByIsAvailableForRentTrue(pageable).getContent();
    }


    public Page<SportWear> findAvailableForSale(Pageable pageable) {
        return sportWearRepository.findByIsAvailableForSaleTrue(pageable);
    }
}