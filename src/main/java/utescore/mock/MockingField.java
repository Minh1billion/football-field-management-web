//package utescore.mock;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import utescore.entity.FieldAvailability;
//import utescore.entity.FootballField;
//import utescore.entity.Location;
//import utescore.repository.AccountRepository;
//import utescore.repository.FieldAvailabilityRepository;
//import utescore.repository.FootballFieldRepository;
//import utescore.repository.LocationRepository;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//
//@Component
//@Order(2)
//@RequiredArgsConstructor
//public class MockingField implements CommandLineRunner {
//    private final FootballFieldRepository footballFieldRepository;
//    private final FieldAvailabilityRepository fieldAvailabilityRepository;
//    private final LocationRepository locationRepository;
//    private final AccountRepository accountRepository;
//
//    private static final String IMAGE_PATH = "D:\\Z3\\fallback.png";
//
//    // Mock Locations
//    public static List<Location> getMockLocations() {
//        List<Location> locations = new ArrayList<>();
//
//        Location loc1 = new Location();
//        loc1.setName("Sân bóng Phú Nhuận");
//        loc1.setAddress("123 Nguyễn Văn Trỗi, Phú Nhuận, Hồ Chí Minh");
//        loc1.setDescription("Sân bóng hiện đại với đầy đủ tiện nghi");
//        loc1.setIsActive(true);
//        loc1.setContactPhone("0901234567");
//        loc1.setContactEmail("phunhuan@utescore.com");
//        loc1.setCreatedAt(LocalDateTime.now().minusMonths(6));
//        loc1.setUpdatedAt(LocalDateTime.now());
//
//        Location loc2 = new Location();
//        loc2.setName("Sân bóng Tân Bình");
//        loc2.setAddress("456 Trường Chinh, Tân Bình, Hồ Chí Minh");
//        loc2.setDescription("Khu liên hợp sân bóng lớn nhất quận");
//        loc2.setIsActive(true);
//        loc2.setContactPhone("0901234568");
//        loc2.setContactEmail("tanbinh@utescore.com");
//        loc2.setCreatedAt(LocalDateTime.now().minusMonths(8));
//        loc2.setUpdatedAt(LocalDateTime.now());
//
//        Location loc3 = new Location();
//        loc3.setName("Sân bóng Quận 7");
//        loc3.setAddress("789 Nguyễn Hữu Thọ, Quận 7, Hồ Chí Minh");
//        loc3.setDescription("Sân bóng gần khu đô thị Phú Mỹ Hưng");
//        loc3.setIsActive(true);
//        loc3.setContactPhone("0901234569");
//        loc3.setContactEmail("quan7@utescore.com");
//        loc3.setCreatedAt(LocalDateTime.now().minusMonths(4));
//        loc3.setUpdatedAt(LocalDateTime.now());
//
//        locations.add(loc1);
//        locations.add(loc2);
//        locations.add(loc3);
//
//        return locations;
//    }
//
//    // Mock Football Fields
//    public static List<FootballField> getMockFootballFields(List<Location> locations) {
//        List<FootballField> fields = new ArrayList<>();
//
//        FootballField field1 = new FootballField();
//        field1.setName("Sân số 1 - 5v5");
//        field1.setDescription("Sân cỏ nhân tạo 5 người chất lượng cao");
//        field1.setFieldType(FootballField.FieldType.FIELD_5V5);
//        field1.setCapacity(10);
//        field1.setSurfaceType(FootballField.SurfaceType.ARTIFICIAL_TURF);
//        field1.setPricePerHour(new BigDecimal("300000"));
//        field1.setIsActive(true);
//        field1.setHasLighting(true);
//        field1.setHasChangingRoom(true);
//        field1.setHasParking(true);
//        field1.setImageUrl(IMAGE_PATH);
//        field1.setLocation(locations.get(0));
//        field1.setCreatedAt(LocalDateTime.now().minusMonths(6));
//        field1.setUpdatedAt(LocalDateTime.now());
//
//        FootballField field2 = new FootballField();
//        field2.setName("Sân số 2 - 7v7");
//        field2.setDescription("Sân cỏ tự nhiên 7 người có mái che");
//        field2.setFieldType(FootballField.FieldType.FIELD_7V7);
//        field2.setCapacity(14);
//        field2.setSurfaceType(FootballField.SurfaceType.NATURAL_GRASS);
//        field2.setPricePerHour(new BigDecimal("500000"));
//        field2.setIsActive(true);
//        field2.setHasLighting(true);
//        field2.setHasChangingRoom(true);
//        field2.setHasParking(true);
//        field2.setImageUrl(IMAGE_PATH);
//        field2.setLocation(locations.get(0));
//        field2.setCreatedAt(LocalDateTime.now().minusMonths(6));
//        field2.setUpdatedAt(LocalDateTime.now());
//
//        FootballField field3 = new FootballField();
//        field3.setName("Sân số 1 - 11v11");
//        field3.setDescription("Sân bóng chuẩn 11 người theo tiêu chuẩn FIFA");
//        field3.setFieldType(FootballField.FieldType.FIELD_11V11);
//        field3.setCapacity(22);
//        field3.setSurfaceType(FootballField.SurfaceType.NATURAL_GRASS);
//        field3.setPricePerHour(new BigDecimal("1000000"));
//        field3.setIsActive(true);
//        field3.setHasLighting(true);
//        field3.setHasChangingRoom(true);
//        field3.setHasParking(true);
//        field3.setImageUrl(IMAGE_PATH);
//        field3.setLocation(locations.get(1));
//        field3.setCreatedAt(LocalDateTime.now().minusMonths(8));
//        field3.setUpdatedAt(LocalDateTime.now());
//
//        FootballField field4 = new FootballField();
//        field4.setName("Sân Futsal - Indoor");
//        field4.setDescription("Sân futsal trong nhà điều hòa");
//        field4.setFieldType(FootballField.FieldType.FUTSAL);
//        field4.setCapacity(10);
//        field4.setSurfaceType(FootballField.SurfaceType.INDOOR);
//        field4.setPricePerHour(new BigDecimal("400000"));
//        field4.setIsActive(true);
//        field4.setHasLighting(true);
//        field4.setHasChangingRoom(true);
//        field4.setHasParking(false);
//        field4.setImageUrl(IMAGE_PATH);
//        field4.setLocation(locations.get(2));
//        field4.setCreatedAt(LocalDateTime.now().minusMonths(4));
//        field4.setUpdatedAt(LocalDateTime.now());
//
//        FootballField field5 = new FootballField();
//        field5.setName("Sân số 3 - 5v5");
//        field5.setDescription("Sân cỏ nhân tạo mini");
//        field5.setFieldType(FootballField.FieldType.FIELD_5V5);
//        field5.setCapacity(10);
//        field5.setSurfaceType(FootballField.SurfaceType.ARTIFICIAL_TURF);
//        field5.setPricePerHour(new BigDecimal("280000"));
//        field5.setIsActive(true);
//        field5.setHasLighting(false);
//        field5.setHasChangingRoom(false);
//        field5.setHasParking(true);
//        field5.setImageUrl(IMAGE_PATH);
//        field5.setLocation(locations.get(1));
//        field5.setCreatedAt(LocalDateTime.now().minusMonths(5));
//        field5.setUpdatedAt(LocalDateTime.now());
//
//        fields.add(field1);
//        fields.add(field2);
//        fields.add(field3);
//        fields.add(field4);
//        fields.add(field5);
//
//        return fields;
//    }
//
//    // Mock Field Availabilities
//    public static List<FieldAvailability> getMockFieldAvailabilities(List<FootballField> fields) {
//        List<FieldAvailability> availabilities = new ArrayList<>();
//
//        // Available slots
//        FieldAvailability avail1 = new FieldAvailability();
//        avail1.setStartTime(LocalDateTime.now().plusDays(1).withHour(8).withMinute(0));
//        avail1.setEndTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0));
//        avail1.setIsAvailable(true);
//        avail1.setField(fields.get(0));
//
//        FieldAvailability avail2 = new FieldAvailability();
//        avail2.setStartTime(LocalDateTime.now().plusDays(1).withHour(14).withMinute(0));
//        avail2.setEndTime(LocalDateTime.now().plusDays(1).withHour(16).withMinute(0));
//        avail2.setIsAvailable(true);
//        avail2.setField(fields.get(0));
//
//        // Unavailable slot - maintenance
//        FieldAvailability avail3 = new FieldAvailability();
//        avail3.setStartTime(LocalDateTime.now().plusDays(2).withHour(10).withMinute(0));
//        avail3.setEndTime(LocalDateTime.now().plusDays(2).withHour(12).withMinute(0));
//        avail3.setIsAvailable(false);
//        avail3.setReason("Bảo trì định kỳ");
//        avail3.setField(fields.get(1));
//
//        // Unavailable slot - private event
//        FieldAvailability avail4 = new FieldAvailability();
//        avail4.setStartTime(LocalDateTime.now().plusDays(3).withHour(18).withMinute(0));
//        avail4.setEndTime(LocalDateTime.now().plusDays(3).withHour(20).withMinute(0));
//        avail4.setIsAvailable(false);
//        avail4.setReason("Sự kiện riêng tư");
//        avail4.setField(fields.get(2));
//
//        FieldAvailability avail5 = new FieldAvailability();
//        avail5.setStartTime(LocalDateTime.now().plusDays(1).withHour(16).withMinute(0));
//        avail5.setEndTime(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0));
//        avail5.setIsAvailable(true);
//        avail5.setField(fields.get(3));
//
//        availabilities.add(avail1);
//        availabilities.add(avail2);
//        availabilities.add(avail3);
//        availabilities.add(avail4);
//        availabilities.add(avail5);
//
//        return availabilities;
//    }
//
//    @Override
//    public void run(String... args) throws Exception {
//        Long managerId = accountRepository.findByUsername("manager")
//                .orElseThrow(() -> new RuntimeException("Manager not found"))
//                .getId();
//
//        List<Location> locations = getMockLocations();
//        locationRepository.saveAll(locations);
//
//        List<FootballField> fields = getMockFootballFields(locations);
//        fields.forEach(f -> f.setManagerId(managerId));
//        footballFieldRepository.saveAll(fields);
//
//        List<FieldAvailability> availabilities = getMockFieldAvailabilities(fields);
//        fieldAvailabilityRepository.saveAll(availabilities);
//    }
//}
