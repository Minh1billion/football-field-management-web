package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.dto.PaymentDTO;
import utescore.entity.Booking;
import utescore.entity.Order;
import utescore.entity.Payment;
import utescore.entity.RentalOrder;
import utescore.repository.BookingRepository;
import utescore.repository.OrderRepository;
import utescore.repository.PaymentRepository;
import utescore.repository.RentalOrderRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

	private final PaymentRepository paymentRepository;
	private final RentalOrderRepository rentalOrderRepository;
	private final OrderRepository orderRepository;
    // repo Booking để cập nhật trạng thái booking khi thanh toán xong
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

		Payment saved = paymentRepository.save(payment);
        // NEW: tự động hoàn tất booking nếu có gắn với payment
        markBookingCompletedIfAny(saved);

        return saved;
	}

	public Payment updatePaymentStatus(Long paymentId, Payment.PaymentStatus status) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));

		payment.setStatus(status);

		if (status == Payment.PaymentStatus.COMPLETED && payment.getPaidAt() == null) {
			payment.setPaidAt(LocalDateTime.now());
		}

		Payment saved = paymentRepository.save(payment);
        //khi COMPLETED thì tự động cập nhật Booking -> COMPLETED
        if (status == Payment.PaymentStatus.COMPLETED) {
            markBookingCompletedIfAny(saved);
        }
        return saved;
	}

	public Payment updatePaymentByRentalOrderId(Long rentalOrderId, String transactionId) {
		RentalOrder rentalOrder = rentalOrderRepository.findById(rentalOrderId)
				.orElseThrow(() -> new RuntimeException("RentalOrder not found with ID: " + rentalOrderId));

		Payment payment = rentalOrder.getPayment();
		if (payment == null) {
			throw new RuntimeException("Payment not found for RentalOrder ID: " + rentalOrderId);
		}

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
        //khi COMPLETED thì tự động cập nhật Booking -> COMPLETED
        if (status == Payment.PaymentStatus.COMPLETED) {
            markBookingCompletedIfAny(saved);
        }

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

    // Helper auto-complete Booking
    private void markBookingCompletedIfAny(Payment payment) {
        if (payment.getBooking() == null) return;

        Booking booking = payment.getBooking();
        try {
            // Luôn chuyển sang COMPLETED khi thanh toán thành công
            booking.setStatus(Booking.BookingStatus.COMPLETED);
            bookingRepository.save(booking);
        } catch (Exception e) {
            // Có thể log nếu cần
            throw new RuntimeException("Không thể cập nhật trạng thái booking sau thanh toán: " + e.getMessage(), e);
        }
    }
}