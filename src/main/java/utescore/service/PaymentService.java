package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.Payment;
import utescore.entity.RentalOrder;
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

	public List<Payment> getAllPayments() {
		return paymentRepository.findAll();
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

	public Payment updatePaymentByRentalOrderId(Long rentalOrderId, String transactionId) {
		RentalOrder rentalOrder = rentalOrderRepository.findById(rentalOrderId)
				.orElseThrow(() -> new RuntimeException("RentalOrder not found with ID: " + rentalOrderId));

		Payment payment = rentalOrder.getPayment();
		if (payment == null) {
			throw new RuntimeException("Payment not found for RentalOrder ID: " + rentalOrderId);
		}

		// Cập nhật trạng thái payment
		payment.setStatus(Payment.PaymentStatus.COMPLETED);
		payment.setTransactionId(transactionId);
		payment.setPaidAt(LocalDateTime.now());

		return paymentRepository.save(payment);
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

		return paymentRepository.save(payment);
	}

	public Payment cancelPayment(Long paymentId, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
			throw new RuntimeException("Cannot cancel completed payment");
		}

		payment.setStatus(Payment.PaymentStatus.CANCELLED);
		String currentNotes = payment.getNotes() != null ? payment.getNotes() : "";
		payment.setNotes(currentNotes + " | Cancelled: " + reason);

		return paymentRepository.save(payment);
	}

	public Payment refundPayment(Long paymentId, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
			throw new RuntimeException("Can only refund completed payments");
		}

		payment.setStatus(Payment.PaymentStatus.REFUNDED);
		String currentNotes = payment.getNotes() != null ? payment.getNotes() : "";
		payment.setNotes(currentNotes + " | Refunded: " + reason);

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

	public Payment getPaymentByRentalOrderId(Long rentalOrderId) {
		RentalOrder rentalOrder = rentalOrderRepository.findById(rentalOrderId)
				.orElseThrow(() -> new RuntimeException("RentalOrder not found with ID: " + rentalOrderId));

		return rentalOrder.getPayment();
	}
}