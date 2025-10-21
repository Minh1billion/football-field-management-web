package utescore.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import utescore.entity.SportWear;

@Repository
public interface SportWearRepository extends JpaRepository<SportWear, Long> {

    @Query("SELECT s FROM SportWear s WHERE " +
            "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:wearType IS NULL OR s.wearType = :wearType) AND " +
            "(:size IS NULL OR s.size = :size)")
    Page<SportWear> findByNameContainingAndWearTypeAndSize(
            @Param("name") String name,
            @Param("wearType") SportWear.WearType wearType,
            @Param("size") SportWear.Size size,
            Pageable pageable);

    Page<SportWear> findByIsAvailableForRentTrue(Pageable pageable);

    Page<SportWear> findByIsAvailableForSaleTrue(Pageable pageable);
}