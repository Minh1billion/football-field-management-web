package utescore.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import utescore.entity.OrderItem;
import utescore.entity.Order;
import utescore.repository.OrderItemRepository;
import utescore.repository.ServiceRepository;
import utescore.repository.SportWearRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderItemService {
    
    private final OrderItemRepository orderItemRepository;
    private final ServiceRepository serviceRepository;
    private final SportWearRepository sportWearRepository;
    
    public List<OrderItem> getAllOrderItems() {
        return orderItemRepository.findAll();
    }
    
    public Optional<OrderItem> getOrderItemById(Long id) {
        return orderItemRepository.findById(id);
    }
    
    public List<OrderItem> getOrderItemsByOrderId(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
    
    public OrderItem createServiceOrderItem(Long serviceId, Integer quantity, BigDecimal unitPrice) {
        utescore.entity.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        
        OrderItem orderItem = new OrderItem();
        orderItem.setService(service);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        orderItem.setItemType(OrderItem.ItemType.SERVICE);
        
        return orderItem;
    }
    
    public OrderItem createSportWearOrderItem(Long sportWearId, Integer quantity, BigDecimal unitPrice) {
        utescore.entity.SportWear sportWear = sportWearRepository.findById(sportWearId)
                .orElseThrow(() -> new RuntimeException("SportWear not found"));
        
        OrderItem orderItem = new OrderItem();
        orderItem.setSportWear(sportWear);
        orderItem.setQuantity(quantity);
        orderItem.setUnitPrice(unitPrice);
        orderItem.setTotalPrice(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        orderItem.setItemType(OrderItem.ItemType.SPORT_WEAR);
        
        return orderItem;
    }
    
    
    public void deleteOrderItem(Long id) {
        orderItemRepository.deleteById(id);
    }
}