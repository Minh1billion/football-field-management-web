package utescore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.*;
import utescore.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ManagerOrderService {

    private final OrderRepository orderRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final BookingRepository bookingRepository;
    private final RentalOrderDetailRepository rentalOrderDetailRepository;
    private final BookingSportWearRepository bookingSportWearRepository;
    private final SportWearRepository sportWearRepository;
    private final LoyaltyRepository loyaltyRepository;

    /**
     * Lấy tất cả đơn hàng (Order, RentalOrder, Booking) với filter
     */
    public List<Map<String, Object>> getAllOrders(String type, String status) {
        List<Map<String, Object>> allOrders = new ArrayList<>();

        if ("all".equals(type) || "sale".equals(type)) {
            allOrders.addAll(getSaleOrders(status));
        }

        if ("all".equals(type) || "rental".equals(type)) {
            allOrders.addAll(getRentalOrders(status));
        }

        if ("all".equals(type) || "booking".equals(type)) {
            allOrders.addAll(getBookings(status));
        }

        // Sắp xếp theo thời gian tạo (mới nhất trước)
        allOrders.sort((a, b) -> {
            LocalDateTime dateA = (LocalDateTime) a.get("createdAt");
            LocalDateTime dateB = (LocalDateTime) b.get("createdAt");
            return dateB.compareTo(dateA);
        });

        return allOrders;
    }

    private List<Map<String, Object>> getSaleOrders(String status) {
        List<Order> orders = "all".equals(status)
                ? orderRepository.findAll()
                : orderRepository.findByStatus(Order.OrderStatus.valueOf(status.toUpperCase()));

        return orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("code", order.getOrderCode());
            map.put("type", "SALE");
            map.put("customerName", order.getCustomerName());
            map.put("totalAmount", order.getTotalAmount());
            map.put("status", order.getStatus().toString());
            map.put("paymentStatus", order.getPayment() != null ?
                    order.getPayment().getStatus().toString() : "N/A");
            map.put("paymentMethod", order.getPayment() != null ?
                    order.getPayment().getPaymentMethod().toString() : "N/A");
            map.put("createdAt", order.getCreatedAt());
            map.put("allReturned", false); // Sale không có trả đồ
            return map;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRentalOrders(String status) {
        List<RentalOrder> orders = rentalOrderRepository.findAll();

        return orders.stream()
                .map(order -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", order.getId());
                    map.put("code", "RO-" + order.getId());
                    map.put("type", "RENTAL");
                    map.put("customerName", order.getCustomerName());
                    map.put("totalAmount", order.getPayment() != null ?
                            order.getPayment().getAmount() : BigDecimal.ZERO);

                    // Kiểm tra trạng thái trả đồ
                    boolean allReturned = order.getOrderDetails() != null &&
                            !order.getOrderDetails().isEmpty() &&
                            order.getOrderDetails().stream()
                                    .allMatch(detail -> detail.getReturnStatus() != null &&
                                            !"RENTED".equals(detail.getReturnStatus()));
                    map.put("allReturned", allReturned);

                    // Set trạng thái dựa trên việc trả đồ
                    String rentalStatus = allReturned ? "COMPLETED" : "ACTIVE";
                    map.put("status", rentalStatus);

                    map.put("paymentStatus", order.getPayment() != null ?
                            order.getPayment().getStatus().toString() : "N/A");
                    map.put("paymentMethod", order.getPayment() != null ?
                            order.getPayment().getPaymentMethod().toString() : "N/A");
                    map.put("createdAt", order.getOrderDate());

                    return map;
                })
                .filter(map -> {
                    // Filter theo status nếu không phải "all"
                    if ("all".equals(status)) {
                        return true;
                    }
                    String orderStatus = (String) map.get("status");
                    return status.equalsIgnoreCase(orderStatus);
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getBookings(String status) {
        List<Booking> bookings = "all".equals(status)
                ? bookingRepository.findAll()
                : bookingRepository.findByStatus(Booking.BookingStatus.valueOf(status.toUpperCase()));

        return bookings.stream().map(booking -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", booking.getId());
            map.put("code", booking.getBookingCode());
            map.put("type", "BOOKING");
            map.put("customerName", booking.getCustomer().getFullName());
            map.put("totalAmount", booking.getTotalAmount());
            map.put("status", booking.getStatus().toString());
            map.put("paymentStatus", booking.getPayment() != null ?
                    booking.getPayment().getStatus().toString() : "N/A");
            map.put("paymentMethod", booking.getPayment() != null ?
                    booking.getPayment().getPaymentMethod().toString() : "N/A");
            map.put("createdAt", booking.getCreatedAt());

            // Kiểm tra trạng thái trả đồ
            boolean allReturned = booking.getBookingSportWears() != null &&
                    !booking.getBookingSportWears().isEmpty() &&
                    booking.getBookingSportWears().stream()
                            .allMatch(bsw -> bsw.getStatus() == BookingSportWear.RentalStatus.RETURNED ||
                                    bsw.getStatus() == BookingSportWear.RentalStatus.DAMAGED ||
                                    bsw.getStatus() == BookingSportWear.RentalStatus.LOST);
            map.put("allReturned", allReturned);

            return map;
        }).collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết đơn mua
     */
    public Order getSaleOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
    }

    /**
     * Lấy chi tiết đơn thuê
     */
    public RentalOrder getRentalOrderById(Long id) {
        return rentalOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thuê"));
    }

    /**
     * Lấy chi tiết booking
     */
    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
    }

    /**
     * Cập nhật trạng thái đơn mua
     */
    public void updateSaleOrderStatus(Long orderId, Order.OrderStatus newStatus) {
        Order order = getSaleOrderById(orderId);

        order.setStatus(newStatus);

        // Nếu chuyển sang DELIVERED, set thời gian giao hàng
        if (newStatus == Order.OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        orderRepository.save(order);
        log.info("Updated sale order {} status to {}", orderId, newStatus);
    }

    /**
     * Xác nhận thanh toán COD cho đơn mua
     */
    public void confirmSaleOrderPayment(Long orderId) {
        Order order = getSaleOrderById(orderId);
        Payment payment = order.getPayment();

        if (payment == null) {
            throw new RuntimeException("Đơn hàng không có thông tin thanh toán");
        }

        if (payment.getPaymentMethod() != Payment.PaymentMethod.COD) {
            throw new RuntimeException("Chỉ có thể xác nhận thanh toán COD");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Thanh toán đã được xử lý");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        // Tích điểm loyalty
        updateLoyaltyPoints(payment);

        orderRepository.save(order);
        log.info("Confirmed COD payment for sale order {}", orderId);
    }

    /**
     * Xác nhận thanh toán COD cho đơn thuê
     */
    public void confirmRentalOrderPayment(Long orderId) {
        RentalOrder order = getRentalOrderById(orderId);
        Payment payment = order.getPayment();

        if (payment == null) {
            throw new RuntimeException("Đơn thuê không có thông tin thanh toán");
        }

        if (payment.getPaymentMethod() != Payment.PaymentMethod.COD) {
            throw new RuntimeException("Chỉ có thể xác nhận thanh toán COD");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Thanh toán đã được xử lý");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        // Tích điểm loyalty
        updateLoyaltyPoints(payment);

        rentalOrderRepository.save(order);
        log.info("Confirmed COD payment for rental order {}", orderId);
    }

    /**
     * Xác nhận thanh toán CASH/COD cho booking
     */
    public void confirmBookingPayment(Long bookingId) {
        Booking booking = getBookingById(bookingId);
        Payment payment = booking.getPayment();

        if (payment == null) {
            throw new RuntimeException("Booking không có thông tin thanh toán");
        }

        if (payment.getPaymentMethod() != Payment.PaymentMethod.COD &&
                payment.getPaymentMethod() != Payment.PaymentMethod.CASH) {
            throw new RuntimeException("Chỉ có thể xác nhận thanh toán CASH/COD");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Thanh toán đã được xử lý");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        // Tích điểm loyalty
        updateLoyaltyPoints(payment);

        bookingRepository.save(booking);
        log.info("Confirmed payment for booking {}", bookingId);
    }

    /**
     * Cập nhật trạng thái trả đồ cho RentalOrderDetail
     */
    public void updateRentalItemStatus(Long detailId, BookingSportWear.RentalStatus status) {
        RentalOrderDetail detail = rentalOrderDetailRepository.findById(detailId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi tiết đơn thuê"));

        detail.setReturnStatus(status.toString());

        // Nếu trả đồ, cập nhật lại tồn kho
        if (status == BookingSportWear.RentalStatus.RETURNED) {
            SportWear sportWear = detail.getSportWear();
            sportWear.setStockQuantity(sportWear.getStockQuantity() + detail.getQuantity());
            sportWearRepository.save(sportWear);
            log.info("Returned stock for sport wear {}: +{}", sportWear.getId(), detail.getQuantity());
        }

        rentalOrderDetailRepository.save(detail);
        log.info("Updated rental item {} status to {}", detailId, status);
    }

    /**
     * Cập nhật trạng thái trả đồ cho BookingSportWear
     */
    public void updateBookingSportWearStatus(Long bookingId, Long sportWearId,
                                             BookingSportWear.RentalStatus status) {
        Booking booking = getBookingById(bookingId);

        BookingSportWear bsw = booking.getBookingSportWears().stream()
                .filter(item -> item.getSportWear().getId().equals(sportWearId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đồ thuê trong booking"));

        bsw.setStatus(status);

        // Nếu trả đồ, cập nhật lại tồn kho
        if (status == BookingSportWear.RentalStatus.RETURNED) {
            SportWear sportWear = bsw.getSportWear();
            sportWear.setStockQuantity(sportWear.getStockQuantity() + bsw.getQuantity());
            sportWearRepository.save(sportWear);
            log.info("Returned stock for sport wear {}: +{}", sportWear.getId(), bsw.getQuantity());
        }

        bookingSportWearRepository.save(bsw);
        log.info("Updated booking sport wear status to {}", status);
    }

    /**
     * Trả tất cả đồ của đơn thuê
     */
    public void returnAllRentalItems(Long orderId) {
        RentalOrder order = getRentalOrderById(orderId);

        for (RentalOrderDetail detail : order.getOrderDetails()) {
            if (!"RETURNED".equals(detail.getReturnStatus())) {
                updateRentalItemStatus(detail.getId(), BookingSportWear.RentalStatus.RETURNED);
            }
        }

        log.info("Returned all items for rental order {}", orderId);
    }

    /**
     * Trả tất cả đồ của booking
     */
    public void returnAllBookingSportWears(Long bookingId) {
        Booking booking = getBookingById(bookingId);

        for (BookingSportWear bsw : booking.getBookingSportWears()) {
            if (bsw.getStatus() == BookingSportWear.RentalStatus.RENTED) {
                bsw.setStatus(BookingSportWear.RentalStatus.RETURNED);

                SportWear sportWear = bsw.getSportWear();
                sportWear.setStockQuantity(sportWear.getStockQuantity() + bsw.getQuantity());
                sportWearRepository.save(sportWear);

                bookingSportWearRepository.save(bsw);
            }
        }

        log.info("Returned all sport wears for booking {}", bookingId);
    }

    /**
     * Tích điểm loyalty khi thanh toán
     */
    private void updateLoyaltyPoints(Payment payment) {
        Customer customer = getCustomerFromPayment(payment);

        if (customer != null) {
            Loyalty loyalty = loyaltyRepository.findByCustomer_Id(customer.getId())
                    .orElseGet(() -> {
                        Loyalty newLoyalty = new Loyalty();
                        newLoyalty.setCustomer(customer);
                        return loyaltyRepository.save(newLoyalty);
                    });

            int pointsToAdd = payment.getAmount().divide(BigDecimal.valueOf(1000)).intValue();

            loyalty.addPoints(pointsToAdd);
            loyalty.setTotalSpent(loyalty.getTotalSpent().add(payment.getAmount()));
            loyalty.setTotalBookings(loyalty.getTotalBookings() + 1);

            loyaltyRepository.save(loyalty);
            log.info("Added {} loyalty points to customer {}", pointsToAdd, customer.getId());
        }
    }

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