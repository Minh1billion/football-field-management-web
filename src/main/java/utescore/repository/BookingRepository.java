package utescore.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import utescore.entity.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
