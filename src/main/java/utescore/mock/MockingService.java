package utescore.mock;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import utescore.entity.Service;
import utescore.entity.SportWear;
import utescore.repository.ServiceRepository;
import utescore.repository.SportWearRepository;
import utescore.service.CloudinaryService;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(3)
@RequiredArgsConstructor
public class MockingService implements CommandLineRunner {
    private static final String IMAGE_PATH = "src/main/resources/static/fallback.png";

    private final ServiceRepository serviceRepository;
    private final SportWearRepository sportWearRepository;
    private final CloudinaryService cloudinaryService;

    // Mock Services
    public static List<Service> getMockServices() {
        List<Service> services = new ArrayList<>();

        Service service1 = new Service();
        service1.setName("Nước suối");
        service1.setDescription("Nước suối Aquafina 500ml");
        service1.setServiceType(Service.ServiceType.DRINK);
        service1.setPrice(new BigDecimal("10000"));
        service1.setIsAvailable(true);
        service1.setImageUrl(IMAGE_PATH);
        service1.setStockQuantity(100);
        service1.setCreatedAt(LocalDateTime.now().minusMonths(6));
        service1.setUpdatedAt(LocalDateTime.now());

        Service service2 = new Service();
        service2.setName("Nước tăng lực");
        service2.setDescription("Red Bull 250ml");
        service2.setServiceType(Service.ServiceType.DRINK);
        service2.setPrice(new BigDecimal("25000"));
        service2.setIsAvailable(true);
        service2.setImageUrl(IMAGE_PATH);
        service2.setStockQuantity(50);
        service2.setCreatedAt(LocalDateTime.now().minusMonths(6));
        service2.setUpdatedAt(LocalDateTime.now());

        Service service3 = new Service();
        service3.setName("Bánh mì");
        service3.setDescription("Bánh mì thịt đặc biệt");
        service3.setServiceType(Service.ServiceType.FOOD);
        service3.setPrice(new BigDecimal("30000"));
        service3.setIsAvailable(true);
        service3.setImageUrl(IMAGE_PATH);
        service3.setStockQuantity(30);
        service3.setCreatedAt(LocalDateTime.now().minusMonths(5));
        service3.setUpdatedAt(LocalDateTime.now());

        Service service4 = new Service();
        service4.setName("Dịch vụ trọng tài");
        service4.setDescription("Trọng tài chuyên nghiệp cho trận đấu");
        service4.setServiceType(Service.ServiceType.REFEREE);
        service4.setPrice(new BigDecimal("200000"));
        service4.setIsAvailable(true);
        service4.setImageUrl(IMAGE_PATH);
        service4.setStockQuantity(0);
        service4.setCreatedAt(LocalDateTime.now().minusMonths(6));
        service4.setUpdatedAt(LocalDateTime.now());

        Service service5 = new Service();
        service5.setName("Thuê bóng đá");
        service5.setDescription("Bóng đá Molten F5A1000 chất lượng cao");
        service5.setServiceType(Service.ServiceType.EQUIPMENT);
        service5.setPrice(new BigDecimal("50000"));
        service5.setIsAvailable(true);
        service5.setImageUrl(IMAGE_PATH);
        service5.setStockQuantity(20);
        service5.setCreatedAt(LocalDateTime.now().minusMonths(6));
        service5.setUpdatedAt(LocalDateTime.now());

        Service service6 = new Service();
        service6.setName("Chụp ảnh trận đấu");
        service6.setDescription("Dịch vụ chụp ảnh chuyên nghiệp toàn trận");
        service6.setServiceType(Service.ServiceType.PHOTOGRAPHY);
        service6.setPrice(new BigDecimal("500000"));
        service6.setIsAvailable(true);
        service6.setImageUrl(IMAGE_PATH);
        service6.setStockQuantity(0);
        service6.setCreatedAt(LocalDateTime.now().minusMonths(4));
        service6.setUpdatedAt(LocalDateTime.now());

        Service service7 = new Service();
        service7.setName("Livestream trận đấu");
        service7.setDescription("Dịch vụ livestream HD chuyên nghiệp");
        service7.setServiceType(Service.ServiceType.LIVESTREAM);
        service7.setPrice(new BigDecimal("800000"));
        service7.setIsAvailable(true);
        service7.setImageUrl(IMAGE_PATH);
        service7.setStockQuantity(0);
        service7.setCreatedAt(LocalDateTime.now().minusMonths(3));
        service7.setUpdatedAt(LocalDateTime.now());

        Service service8 = new Service();
        service8.setName("Cơm hộp");
        service8.setDescription("Cơm hộp dinh dưỡng sau trận");
        service8.setServiceType(Service.ServiceType.FOOD);
        service8.setPrice(new BigDecimal("45000"));
        service8.setIsAvailable(true);
        service8.setImageUrl(IMAGE_PATH);
        service8.setStockQuantity(40);
        service8.setCreatedAt(LocalDateTime.now().minusMonths(5));
        service8.setUpdatedAt(LocalDateTime.now());

        services.add(service1);
        services.add(service2);
        services.add(service3);
        services.add(service4);
        services.add(service5);
        services.add(service6);
        services.add(service7);
        services.add(service8);

        return services;
    }

    // Mock Sport Wears
    public static List<SportWear> getMockSportWears() {
        List<SportWear> sportWears = new ArrayList<>();

        SportWear wear1 = new SportWear();
        wear1.setName("Áo đấu Nike");
        wear1.setDescription("Áo đấu Nike Dri-FIT chính hãng");
        wear1.setWearType(SportWear.WearType.JERSEY);
        wear1.setSize(SportWear.Size.L);
        wear1.setColor("Đỏ");
        wear1.setRentalPricePerDay(new BigDecimal("50000"));
        wear1.setSellPrice(new BigDecimal("350000"));
        wear1.setStockQuantity(15);
        wear1.setIsAvailableForRent(true);
        wear1.setIsAvailableForSale(true);
        wear1.setImageUrl(IMAGE_PATH);
        wear1.setBrand("Nike");
        wear1.setCreatedAt(LocalDateTime.now().minusMonths(6));
        wear1.setUpdatedAt(LocalDateTime.now());

        SportWear wear2 = new SportWear();
        wear2.setName("Áo đấu Adidas");
        wear2.setDescription("Áo đấu Adidas Tiro chính hãng");
        wear2.setWearType(SportWear.WearType.JERSEY);
        wear2.setSize(SportWear.Size.M);
        wear2.setColor("Xanh dương");
        wear2.setRentalPricePerDay(new BigDecimal("50000"));
        wear2.setSellPrice(new BigDecimal("380000"));
        wear2.setStockQuantity(20);
        wear2.setIsAvailableForRent(true);
        wear2.setIsAvailableForSale(true);
        wear2.setImageUrl(IMAGE_PATH);
        wear2.setBrand("Adidas");
        wear2.setCreatedAt(LocalDateTime.now().minusMonths(6));
        wear2.setUpdatedAt(LocalDateTime.now());

        SportWear wear3 = new SportWear();
        wear3.setName("Quần đùi Nike");
        wear3.setDescription("Quần đùi Nike Dri-FIT");
        wear3.setWearType(SportWear.WearType.SHORTS);
        wear3.setSize(SportWear.Size.L);
        wear3.setColor("Đen");
        wear3.setRentalPricePerDay(new BigDecimal("30000"));
        wear3.setSellPrice(new BigDecimal("200000"));
        wear3.setStockQuantity(25);
        wear3.setIsAvailableForRent(true);
        wear3.setIsAvailableForSale(true);
        wear3.setImageUrl(IMAGE_PATH);
        wear3.setBrand("Nike");
        wear3.setCreatedAt(LocalDateTime.now().minusMonths(6));
        wear3.setUpdatedAt(LocalDateTime.now());

        SportWear wear4 = new SportWear();
        wear4.setName("Tất bóng đá");
        wear4.setDescription("Tất bóng đá chống trượt");
        wear4.setWearType(SportWear.WearType.SOCKS);
        wear4.setSize(SportWear.Size.M);
        wear4.setColor("Trắng");
        wear4.setRentalPricePerDay(new BigDecimal("15000"));
        wear4.setSellPrice(new BigDecimal("50000"));
        wear4.setStockQuantity(50);
        wear4.setIsAvailableForRent(true);
        wear4.setIsAvailableForSale(true);
        wear4.setImageUrl(IMAGE_PATH);
        wear4.setBrand("Mitre");
        wear4.setCreatedAt(LocalDateTime.now().minusMonths(5));
        wear4.setUpdatedAt(LocalDateTime.now());

        SportWear wear5 = new SportWear();
        wear5.setName("Giày đá bóng Nike Mercurial");
        wear5.setDescription("Giày đá bóng Nike Mercurial Vapor");
        wear5.setWearType(SportWear.WearType.SHOES);
        wear5.setSize(SportWear.Size.XL);
        wear5.setColor("Xanh lá");
        wear5.setRentalPricePerDay(new BigDecimal("100000"));
        wear5.setSellPrice(new BigDecimal("2500000"));
        wear5.setStockQuantity(8);
        wear5.setIsAvailableForRent(true);
        wear5.setIsAvailableForSale(true);
        wear5.setImageUrl(IMAGE_PATH);
        wear5.setBrand("Nike");
        wear5.setCreatedAt(LocalDateTime.now().minusMonths(4));
        wear5.setUpdatedAt(LocalDateTime.now());

        SportWear wear6 = new SportWear();
        wear6.setName("Ống đồng bảo vệ");
        wear6.setDescription("Ống đồng bảo vệ chống va chạm");
        wear6.setWearType(SportWear.WearType.SHIN_GUARDS);
        wear6.setSize(SportWear.Size.M);
        wear6.setColor("Đen");
        wear6.setRentalPricePerDay(new BigDecimal("20000"));
        wear6.setSellPrice(new BigDecimal("100000"));
        wear6.setStockQuantity(30);
        wear6.setIsAvailableForRent(true);
        wear6.setIsAvailableForSale(true);
        wear6.setImageUrl(IMAGE_PATH);
        wear6.setBrand("Adidas");
        wear6.setCreatedAt(LocalDateTime.now().minusMonths(5));
        wear6.setUpdatedAt(LocalDateTime.now());

        SportWear wear7 = new SportWear();
        wear7.setName("Găng tay thủ môn");
        wear7.setDescription("Găng tay thủ môn chuyên nghiệp");
        wear7.setWearType(SportWear.WearType.GLOVES);
        wear7.setSize(SportWear.Size.L);
        wear7.setColor("Vàng đen");
        wear7.setRentalPricePerDay(new BigDecimal("40000"));
        wear7.setSellPrice(new BigDecimal("400000"));
        wear7.setStockQuantity(10);
        wear7.setIsAvailableForRent(true);
        wear7.setIsAvailableForSale(true);
        wear7.setImageUrl(IMAGE_PATH);
        wear7.setBrand("Adidas");
        wear7.setCreatedAt(LocalDateTime.now().minusMonths(4));
        wear7.setUpdatedAt(LocalDateTime.now());

        SportWear wear8 = new SportWear();
        wear8.setName("Bộ thủ môn Puma");
        wear8.setDescription("Bộ đồ thủ môn Puma chuyên dụng");
        wear8.setWearType(SportWear.WearType.GOALKEEPER_KIT);
        wear8.setSize(SportWear.Size.L);
        wear8.setColor("Cam");
        wear8.setRentalPricePerDay(new BigDecimal("80000"));
        wear8.setSellPrice(new BigDecimal("600000"));
        wear8.setStockQuantity(5);
        wear8.setIsAvailableForRent(true);
        wear8.setIsAvailableForSale(true);
        wear8.setImageUrl(IMAGE_PATH);
        wear8.setBrand("Puma");
        wear8.setCreatedAt(LocalDateTime.now().minusMonths(3));
        wear8.setUpdatedAt(LocalDateTime.now());

        sportWears.add(wear1);
        sportWears.add(wear2);
        sportWears.add(wear3);
        sportWears.add(wear4);
        sportWears.add(wear5);
        sportWears.add(wear6);
        sportWears.add(wear7);
        sportWears.add(wear8);

        return sportWears;
    }

    @Override
    public void run(String... args) throws Exception {
        File fallbackImage = new File("C:\\Users\\ADMIN\\Desktop\\web-programming\\images\\services\\fallback.png");
        String uploadedImageName = cloudinaryService.uploadAndGetName(fallbackImage);
        String uploadedImageUrl = cloudinaryService.getImageUrl(uploadedImageName);

        List<Service> services = getMockServices();
        for (Service s : services) {
            // ✅ Set trực tiếp, không cần kiểm tra
            s.setImageUrl(uploadedImageUrl);
        }
        serviceRepository.saveAll(services);

        List<SportWear> sportWears = getMockSportWears();
        for (SportWear w : sportWears) {
            // ✅ Set trực tiếp, không cần kiểm tra
            w.setImageUrl(uploadedImageUrl);
        }
        sportWearRepository.saveAll(sportWears);
    }
}
