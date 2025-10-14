package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.Payment;
import utescore.entity.Order;
import utescore.entity.Booking;
import utescore.repository.PaymentRepository;
import utescore.repository.OrderRepository;
import utescore.repository.BookingRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final OrderRepository orderRepository;
	private final BookingRepository bookingRepository;

	public List<Payment> getAllPayments() {
		return paymentRepository.findAll();
	}

	public Payment createPaymentForOrder(Long orderId, Payment.PaymentMethod paymentMethod, String notes) {
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

		Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
		if (existingPayment.isPresent()) {
			throw new RuntimeException("Payment already exists for this order");
		}

		Payment payment = new Payment();
		payment.setPaymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		payment.setAmount(order.getTotalAmount());
		payment.setPaymentMethod(paymentMethod);
		payment.setStatus(Payment.PaymentStatus.PENDING);
		payment.setNotes(notes);
		payment.setOrder(order);

		return paymentRepository.save(payment);
	}

	public Payment createPaymentForBooking(Long bookingId, BigDecimal amount, Payment.PaymentMethod paymentMethod,
			String notes) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));

		Optional<Payment> existingPayment = paymentRepository.findByBookingId(bookingId);
		if (existingPayment.isPresent()) {
			throw new RuntimeException("Payment already exists for this booking");
		}

		Payment payment = new Payment();
		payment.setPaymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		payment.setAmount(amount);
		payment.setPaymentMethod(paymentMethod);
		payment.setStatus(Payment.PaymentStatus.PENDING);
		payment.setNotes(notes);
		payment.setBooking(booking);

		return paymentRepository.save(payment);
	}

	public Payment createPaymentForBookingAndOrder(Long bookingId, Long orderId, BigDecimal amount,
			Payment.PaymentMethod paymentMethod, String notes) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new RuntimeException("Booking not found with ID: " + bookingId));
		Order order = orderRepository.findById(orderId)
				.orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
		Optional<Payment> existingPayment = paymentRepository.findByBookingId(bookingId);
		if (existingPayment.isPresent()) {
			throw new RuntimeException("Payment already exists for this booking");
		}
		Payment payment = new Payment();
		payment.setPaymentCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		payment.setAmount(amount);
		payment.setPaymentMethod(paymentMethod);
		payment.setStatus(Payment.PaymentStatus.PENDING);
		payment.setNotes(notes);
		payment.setBooking(booking);
		payment.setOrder(order);
		return paymentRepository.save(payment);
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

		return paymentRepository.save(payment);
	}

	public Payment updatePaymentStatus(Long paymentId, Payment.PaymentStatus status) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		payment.setStatus(status);

		if (status == Payment.PaymentStatus.COMPLETED && payment.getPaidAt() == null) {
			payment.setPaidAt(LocalDateTime.now());
		}

		return paymentRepository.save(payment);
	}

	public Payment cancelPayment(Long paymentId, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
			throw new RuntimeException("Cannot cancel completed payment");
		}

		payment.setStatus(Payment.PaymentStatus.CANCELLED);
		payment.setNotes(payment.getNotes() + " | Cancelled: " + reason);

		return paymentRepository.save(payment);
	}

	public Payment refundPayment(Long paymentId, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
			throw new RuntimeException("Can only refund completed payments");
		}

		payment.setStatus(Payment.PaymentStatus.REFUNDED);
		payment.setNotes(payment.getNotes() + " | Refunded: " + reason);

		return paymentRepository.save(payment);
	}

	public Payment updatePaymentAmount(Long paymentId, BigDecimal newAmount) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
			throw new RuntimeException("Cannot update amount for completed payment");
		}

		payment.setAmount(newAmount);
		return paymentRepository.save(payment);
	}
}