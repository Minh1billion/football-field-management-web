package utescore.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import utescore.entity.Booking;
import utescore.repository.BookingRepository;

@Service
public class BooKingService {
	@Autowired
	private BookingRepository bookingRepository;
	
	public List<Booking> getAllBookings() {
		return bookingRepository.findAll();
	}
	
}
