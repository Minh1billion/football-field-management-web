package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.PaymentDTO;
import utescore.entity.*;
import utescore.repository.LoyaltyRepository;
import utescore.entity.Booking;
import utescore.entity.Order;
import utescore.entity.Payment;
import utescore.entity.RentalOrder;
import utescore.repository.BookingRepository;
import utescore.repository.OrderRepository;
import utescore.repository.PaymentRepository;
import utescore.repository.RentalOrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final RentalOrderRepository rentalOrderRepository;
	private final OrderRepository orderRepository;
	private final LoyaltyRepository loyaltyRepository;
	private final BookingRepository bookingRepository;

	public List<PaymentDTO> getAllPayments() {
		return paymentRepository.findAll().stream().map(this::convertToDTO).toList();
	}

	public PaymentDTO getPaymentById(Long id) {
		return paymentRepository.findById(id).map(this::convertToDTO).orElse(null);
	}

	public List<PaymentDTO> getPaymentsByCustomerId(Long customerId) {
		return paymentRepository.findByCustomerId(customerId).stream()
				.map(this::convertToDTO)
				.toList();
	}

	public PaymentDTO getPaymentByIdAndCustomerId(Long paymentId, Long customerId) {
		return paymentRepository.findByIdAndCustomerId(paymentId, customerId)
				.map(this::convertToDTO)
				.orElse(null);
	}

	public Payment processPayment(Long paymentId, String transactionId) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
			throw new RuntimeException("Payment is not in pending status");
		}

		payment.setStatus(Payment.PaymentStatus.COMPLETED);
		payment.setTransactionId(transactionId);
		payment.setPaidAt(LocalDateTime.now());

		Payment savedPayment = paymentRepository.save(payment);

		// ✅ QUAN TRỌNG: Chỉ CONFIRM booking, KHÔNG auto-complete
		confirmBookingIfPending(savedPayment);

		updateLoyaltyPoints(payment);

		return savedPayment;
	}

	public Payment updatePaymentStatus(Long paymentId, Payment.PaymentStatus status) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		payment.setStatus(status);

		if (status == Payment.PaymentStatus.COMPLETED && payment.getPaidAt() == null) {
			payment.setPaidAt(LocalDateTime.now());
		}

		Payment saved = paymentRepository.save(payment);

		if (status == Payment.PaymentStatus.COMPLETED) {
			// ✅ Chỉ CONFIRM, không auto-complete
			confirmBookingIfPending(saved);
		}

		updateLoyaltyPoints(payment);

		return saved;
	}

	public Payment updatePaymentByRentalOrderId(Long rentalOrderId, String transactionId) {
		RentalOrder rentalOrder = rentalOrderRepository.findById(rentalOrderId)
				.orElseThrow(() -> new RuntimeException("RentalOrder not found with ID: " + rentalOrderId));

		Payment payment = rentalOrder.getPayment();
		if (payment == null) {
			throw new RuntimeException("Payment not found for RentalOrder ID: " + rentalOrderId);
		}

		updateLoyaltyPoints(payment);

		payment.setStatus(Payment.PaymentStatus.COMPLETED);
		payment.setTransactionId(transactionId);
		payment.setPaidAt(LocalDateTime.now());

		return paymentRepository.save(payment);
	}

	@Transactional
	public void updatePaymentByOrderId(Long orderId, String transactionId) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

		Payment payment = order.getPayment();
		if (payment != null) {
			payment.setPaymentCode(transactionId);
			payment.setStatus(Payment.PaymentStatus.COMPLETED);
			payment.setPaidAt(LocalDateTime.now());

			updateLoyaltyPoints(payment);

			paymentRepository.save(payment);
		}
	}

	public Payment findByPaymentCode(String paymentCode) {
		return paymentRepository.findByPaymentCode(paymentCode)
				.orElseThrow(() -> new RuntimeException("Payment not found with code: " + paymentCode));
	}

	public Payment updatePaymentStatusByCode(String paymentCode, Payment.PaymentStatus status, String transactionId) {
		Payment payment = findByPaymentCode(paymentCode);

		payment.setStatus(status);
		if (transactionId != null) {
			payment.setTransactionId(transactionId);
		}

		if (status == Payment.PaymentStatus.COMPLETED && payment.getPaidAt() == null) {
			payment.setPaidAt(LocalDateTime.now());
		}

		Payment saved = paymentRepository.save(payment);

		if (status == Payment.PaymentStatus.COMPLETED) {
			// ✅ Chỉ CONFIRM, không auto-complete
			confirmBookingIfPending(saved);
		}

		updateLoyaltyPoints(payment);

		return saved;
	}

	public Payment findById(Long id) {
		return paymentRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy payment"));
	}

	@Transactional
	public Payment save(Payment payment) {
		return paymentRepository.save(payment);
	}

	private PaymentDTO convertToDTO(Payment payment) {
		PaymentDTO dto = new PaymentDTO();
		dto.setId(payment.getId());
		dto.setPaymentCode(payment.getPaymentCode());
		dto.setPaymentMethod(payment.getPaymentMethod().toString());
		dto.setAmount(payment.getAmount());
		dto.setStatus(payment.getStatus().toString());
		dto.setNotes(payment.getNotes());
		dto.setCreatedAt(payment.getCreatedAt());
		dto.setPaidAt(payment.getPaidAt());

		if (payment.getOrder() != null) {
			dto.setOrderId(payment.getOrder().getId());
		}

		if (payment.getRentalOrder() != null) {
			dto.setRentalOrderId(payment.getRentalOrder().getId());
		}

		if (payment.getBooking() != null) {
			dto.setBookingId(payment.getBooking().getId());
			if (payment.getBooking().getCustomer() != null) {
				dto.setCustomerId(payment.getBooking().getCustomer().getId());
				dto.setCustomerName(payment.getBooking().getCustomer().getFullName());
			}
		} else if (payment.getRentalOrder() != null && payment.getRentalOrder().getCustomer() != null) {
			dto.setCustomerId(payment.getRentalOrder().getCustomer().getId());
			dto.setCustomerName(payment.getRentalOrder().getCustomer().getFullName());
		} else if (payment.getOrder() != null && payment.getOrder().getCustomer() != null) {
			dto.setCustomerId(payment.getOrder().getCustomer().getId());
			dto.setCustomerName(payment.getOrder().getCustomer().getFullName());
		}

		return dto;
	}

	// ✅ FIXED: Chỉ chuyển sang CONFIRMED khi thanh toán, KHÔNG auto-complete
	private void confirmBookingIfPending(Payment payment) {
		if (payment.getBooking() == null) return;

		Booking booking = payment.getBooking();

		// Chỉ chuyển từ PENDING sang CONFIRMED
		if (booking.getStatus() == Booking.BookingStatus.PENDING) {
			booking.setStatus(Booking.BookingStatus.CONFIRMED);
			bookingRepository.save(booking);
		}
		// Không làm gì nếu đã CONFIRMED hoặc COMPLETED
	}

	// Phương thức private để cập nhật điểm thưởng
	private void updateLoyaltyPoints(Payment payment) {
		Customer customer = getCustomerFromPayment(payment);

		if (customer != null) {
			Loyalty loyalty = loyaltyRepository.findByCustomer_Id(customer.getId())
					.orElseGet(() -> {
						// Tạo mới nếu chưa có
						Loyalty newLoyalty = new Loyalty();
						newLoyalty.setCustomer(customer);
						return loyaltyRepository.save(newLoyalty);
					});

			int pointsToAdd = payment.getAmount().divide(BigDecimal.valueOf(1000)).intValue();

			loyalty.addPoints(pointsToAdd);
			loyalty.setTotalSpent(loyalty.getTotalSpent().add(payment.getAmount()));
			loyalty.setTotalBookings(loyalty.getTotalBookings() + 1);

			loyaltyRepository.save(loyalty);
		}
	}

	// Phương thức helper để lấy Customer từ Payment
	private Customer getCustomerFromPayment(Payment payment) {
		if (payment.getBooking() != null && payment.getBooking().getCustomer() != null) {
			return payment.getBooking().getCustomer();
		} else if (payment.getRentalOrder() != null && payment.getRentalOrder().getCustomer() != null) {
			return payment.getRentalOrder().getCustomer();
		} else if (payment.getOrder() != null && payment.getOrder().getCustomer() != null) {
			return payment.getOrder().getCustomer();
		}
		return null;
	}
}