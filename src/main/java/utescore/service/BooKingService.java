package utescore.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import utescore.dto.BookingServiceReq;
import utescore.dto.BookingSportWearReq;
import utescore.entity.Booking;
import utescore.entity.FootballField;
import utescore.entity.Service;
import utescore.repository.BookingRepository;
import utescore.repository.BookingServiceRepository;
import utescore.repository.BookingSportWearRep;
import utescore.repository.SportWearRepository;

@org.springframework.stereotype.Service
public class BooKingService {
	@Autowired
	private BookingRepository bookingRepository;
	@Autowired
	private BookingServiceRepository bookingServiceRepository;
	@Autowired
	private BookingSportWearRep bookingSportWearRepository;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private ServiceService serviceService;
	@Autowired
	private SportWearRepository SportWearRepository;
	
	public List<Booking> getAllBookings() {
		return bookingRepository.findAll();
	}
	
	public Booking createBooking (Long customerId,FootballField field, String notes, Integer numberOfPlayers) {
		Booking booking = new Booking();
		booking.setCustomer(customerService.getCustomerById(customerId));
		booking.setField(field);
		booking.setNotes(notes);
		booking.setNumberOfPlayers(numberOfPlayers);
		Booking savedBooking = bookingRepository.save(booking);
		return savedBooking;
	}
	
	public Long BookingService (Long bookingId, List<BookingServiceReq> serviceReqs) {
		for (BookingServiceReq req : serviceReqs) {
			Long idService = req.getServiceId();
			Service service = serviceService.findById(idService);
			Integer quantity = req.getQuantity();
			
			utescore.entity.BookingService bookingService = new utescore.entity.BookingService();
			bookingService.setBooking(bookingRepository.findById(bookingId).orElse(null));
			bookingService.setService(service);
			bookingService.setUnitPrice(service.getPrice());
			bookingService.setTotalPrice(service.getPrice().multiply(new java.math.BigDecimal(quantity)));
			bookingService.setQuantity(quantity);
			bookingServiceRepository.save(bookingService);
		}
		return bookingId;
	}
	
	// true: sell, false: rent
	public Long BookingSportWear (Long bookingId, List<BookingSportWearReq> sportWearReqs) {
		for (BookingSportWearReq req : sportWearReqs) {
			Long idSportWear = req.getSportWearId();
			utescore.entity.SportWear sportWear = SportWearRepository.findById(idSportWear).orElse(null);
			Integer quantity = req.getQuantity();
			
			BigDecimal price = req.isSell() ? sportWear.getSellPrice() : sportWear.getRentalPricePerDay();
			
			utescore.entity.BookingSportWear bookingSportWear = new utescore.entity.BookingSportWear();
			bookingSportWear.setBooking(bookingRepository.findById(bookingId).orElse(null));
			bookingSportWear.setSportWear(sportWear);
			bookingSportWear.setUnitPrice(price);
			bookingSportWear.setTotalPrice(price.multiply(new java.math.BigDecimal(quantity)));
			bookingSportWear.setQuantity(quantity);
			bookingSportWearRepository.save(bookingSportWear);
		}
		return bookingId;
	}
}