package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import utescore.repository.RentalRepository;

@Service
@RequiredArgsConstructor
public class RentalService {
    private final RentalRepository rentalRepository;

    public long countActiveRentals(String username) {
        return rentalRepository.countActiveRentals(username);
    }
}
