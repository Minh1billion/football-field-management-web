package utescore.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "Poster")
@Data
public class Poster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String ten;

    @Column(nullable = false, length = 255)
    private String url;

    @Column(name = "link_chuyen_huong", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String linkChuyenHuong;
}
