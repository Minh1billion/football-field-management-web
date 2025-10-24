package utescore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.dto.CartDTO;
import utescore.dto.SaleDTO;
import utescore.entity.*;
import utescore.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = BigDecimal.valueOf(500000);
    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(30000);

    private final SportWearService sportWearService;
    private final SportWearRepository sportWearRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public Iterable<SportWear> getAvailableSportWearsForSale(Pageable pageable) {
        log.debug("Fetching available sport wears for sale, page: {}", pageable.getPageNumber());
        return sportWearService.findAvailableForSale(pageable);
    }

    public String showSportWearDetail(Long id, Model model, RedirectAttributes redirectAttributes) {
        log.debug("Showing sport wear detail: id={}", id);
        SportWear wear = sportWearService.findById(id);

        if (wear == null) {
            log.warn("Sport wear not found: id={}", id);
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm.");
            return "redirect:/user/sales";
        }

        model.addAttribute("sportWear", wear);
        return "user/sales/detail";
    }

    public String addToCart(CartDTO cartDTO, Long sportWearId, int quantity, RedirectAttributes redirectAttributes) {
        log.info("Adding to cart: sportWearId={}, quantity={}", sportWearId, quantity);

        SportWear wear = sportWearService.findById(sportWearId);

        if (wear == null || !wear.getIsAvailableForSale()) {
            log.warn("Product unavailable: sportWearId={}", sportWearId);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Sản phẩm không tồn tại hoặc không có sẵn để bán.");
            return "redirect:/user/sales/detail/" + sportWearId;
        }

        if (quantity <= 0) {
            log.warn("Invalid quantity: {}", quantity);
            redirectAttributes.addFlashAttribute("errorMessage", "Số lượng phải lớn hơn 0.");
            return "redirect:/user/sales/detail/" + sportWearId;
        }

        if (quantity > wear.getStockQuantity()) {
            log.warn("Insufficient stock: requested={}, available={}", quantity, wear.getStockQuantity());
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Số lượng yêu cầu vượt quá số lượng tồn kho (" + wear.getStockQuantity() + ").");
            return "redirect:/user/sales/detail/" + sportWearId;
        }

        SaleDTO newItem = new SaleDTO();
        newItem.setSportWearId(sportWearId);
        newItem.setName(wear.getName());
        newItem.setUnitPrice(wear.getSellPrice());
        newItem.setQuantity(quantity);
        newItem.setImageUrl(wear.getImageUrl());
        newItem.setTotalPrice(wear.getSellPrice().multiply(BigDecimal.valueOf(quantity)));

        updateCart(cartDTO, newItem);

        log.info("✅ Added to cart successfully: {} x {}", quantity, wear.getName());
        redirectAttributes.addFlashAttribute("successMessage",
                "Đã thêm " + quantity + " x " + wear.getName() + " vào giỏ hàng.");

        return "redirect:/user/sales/cart";
    }

    public void updateCartItem(CartDTO cart, Long sportWearId, int quantity) {
        log.debug("Updating cart item: sportWearId={}, quantity={}", sportWearId, quantity);

        Optional<SaleDTO> itemOpt = cart.findSaleItemById(sportWearId);
        if (itemOpt.isPresent()) {
            SaleDTO item = itemOpt.get();
            item.setQuantity(quantity);
            item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)));
            recalculateCartTotal(cart);
            log.debug("✅ Cart item updated");
        } else {
            log.warn("Cart item not found: sportWearId={}", sportWearId);
        }
    }

    private void updateCart(CartDTO cart, SaleDTO newItem) {
        Optional<SaleDTO> existingItemOpt = cart.findSaleItemById(newItem.getSportWearId());

        if (existingItemOpt.isPresent()) {
            SaleDTO existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + newItem.getQuantity());
            existingItem.setTotalPrice(existingItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(existingItem.getQuantity())));
            log.debug("Updated existing cart item: sportWearId={}", newItem.getSportWearId());
        } else {
            cart.getSaleItems().add(newItem);
            log.debug("Added new cart item: sportWearId={}", newItem.getSportWearId());
        }

        recalculateCartTotal(cart);
    }

    public void recalculateCartTotal(CartDTO cart) {
        // Tính tổng từ rental items
        BigDecimal rentalTotal = cart.getRentalItems().stream()
                .map(item -> item.getRentalPricePerDay()
                        .multiply(BigDecimal.valueOf(item.getRentalDays()))
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tổng từ sale items
        BigDecimal saleTotal = cart.getSaleItems().stream()
                .map(SaleDTO::calculateTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = rentalTotal.add(saleTotal);
        cart.setTotalPrice(total);

        log.debug("Cart total recalculated: rentalTotal={}, saleTotal={}, total={}",
                rentalTotal, saleTotal, total);
    }

    @Transactional
    public Long createSaleOrder(CartDTO cartDTO, String username, String customerName,
                                String customerPhone, String customerEmail, String customerCity,
                                String customerAddress, String notes, String paymentMethod) {

        log.info("🔵 Creating sale order: username={}, paymentMethod={}", username, paymentMethod);

        try {
            // 1. Validate và lấy thông tin account
            Account account = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản: " + username));
            log.debug("✅ Found account: id={}", account.getId());

            Customer customer = account.getCustomer();
            if (customer == null) {
                throw new RuntimeException("Không tìm thấy thông tin khách hàng cho account: " + username);
            }
            log.debug("✅ Found customer: id={}", customer.getId());

            // 2. Tính toán số tiền
            BigDecimal subtotal = cartDTO.getTotalPrice();
            BigDecimal shippingFee = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0
                    ? SHIPPING_FEE
                    : BigDecimal.ZERO;
            BigDecimal totalAmount = subtotal.add(shippingFee);

            log.info("💰 Order amounts: subtotal={}, shippingFee={}, total={}",
                    subtotal, shippingFee, totalAmount);

            // 3. Tạo Order
            String orderCode = generateOrderCode();
            Order order = buildOrder(account, customer, orderCode, customerName, customerPhone,
                    customerEmail, customerCity, customerAddress, notes, shippingFee, totalAmount);

            log.debug("📦 Order created: code={}", orderCode);

            // 4. Tạo Payment
            Payment payment = buildPayment(orderCode, notes, totalAmount, paymentMethod);

            // 5. Link Order và Payment
            order.setPayment(payment);
            payment.setOrder(order);

            // 6. Save Order (cascade save Payment)
            order = orderRepository.save(order);
            log.info("✅ Order saved: id={}, code={}", order.getId(), orderCode);

            // 7. Tạo OrderItems và cập nhật stock
            createOrderItemsAndUpdateStock(order, cartDTO);

            log.info("🎉 Sale order created successfully: id={}, code={}", order.getId(), orderCode);
            return order.getId();

        } catch (Exception e) {
            log.error("❌ Failed to create sale order: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể tạo đơn hàng: " + e.getMessage(), e);
        }
    }

    private Order buildOrder(Account account, Customer customer, String orderCode,
                             String customerName, String customerPhone, String customerEmail,
                             String customerCity, String customerAddress, String notes,
                             BigDecimal shippingFee, BigDecimal totalAmount) {
        Order order = new Order();
        order.setOrderCode(orderCode);
        order.setAccount(account);
        order.setCustomer(customer);
        order.setCustomerName(customerName);
        order.setCustomerPhone(customerPhone);
        order.setCustomerEmail(customerEmail);
        order.setCustomerCity(customerCity);
        order.setCustomerAddress(customerAddress);
        order.setShippingFee(shippingFee);
        order.setTotalAmount(totalAmount);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setNotes(notes);
        return order;
    }

    private Payment buildPayment(String orderCode, String notes, BigDecimal totalAmount, String paymentMethod) {
        Payment payment = new Payment();
        payment.setNotes(notes);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod("COD".equals(paymentMethod)
                ? Payment.PaymentMethod.COD
                : Payment.PaymentMethod.VNPAY);

        if ("COD".equals(paymentMethod)) {
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setPaymentCode("COD-" + orderCode);
            log.debug("💵 COD payment created");
        } else if ("VNPAY".equals(paymentMethod)) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setPaymentCode("VNPAY-" + orderCode);
            payment.setPaidAt(LocalDateTime.now());
            log.debug("💳 VNPay payment created");
        }

        payment.setCreatedAt(LocalDateTime.now());
        return payment;
    }

    private void createOrderItemsAndUpdateStock(Order order, CartDTO cartDTO) {
        log.debug("Creating order items for {} products", cartDTO.getSaleItems().size());

        for (SaleDTO cartItem : cartDTO.getSaleItems()) {
            SportWear sportWear = sportWearRepository.findById(cartItem.getSportWearId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy sản phẩm ID: " + cartItem.getSportWearId()));

            // Kiểm tra tồn kho
            if (cartItem.getQuantity() > sportWear.getStockQuantity()) {
                throw new RuntimeException(String.format(
                        "Sản phẩm %s không đủ số lượng trong kho (yêu cầu: %d, có sẵn: %d)",
                        sportWear.getName(), cartItem.getQuantity(), sportWear.getStockQuantity()));
            }

            // Tạo OrderItem
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setSportWear(sportWear);
            orderItem.setItemType(OrderItem.ItemType.SPORT_WEAR);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setTotalPrice(cartItem.getUnitPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity())));

            orderItemRepository.save(orderItem);
            log.debug("✅ Order item saved: sportWearId={}, quantity={}, itemType=SPORT_WEAR",
                    cartItem.getSportWearId(), cartItem.getQuantity());

            // Cập nhật tồn kho
            int newStock = sportWear.getStockQuantity() - cartItem.getQuantity();
            sportWear.setStockQuantity(newStock);
            sportWearRepository.save(sportWear);
            log.debug("📦 Stock updated: sportWearId={}, newStock={}",
                    sportWear.getId(), newStock);
        }

        log.info("✅ Created {} order items and updated stock", cartDTO.getSaleItems().size());
    }

    private String generateOrderCode() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return "ORD-" + LocalDateTime.now().format(formatter);
    }
}