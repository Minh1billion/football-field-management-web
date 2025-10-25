package utescore.mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import utescore.entity.Poster;
import utescore.repository.PosterRepository;

import java.util.List;

@Component
public class MokingPoster implements CommandLineRunner {

    @Autowired
    private PosterRepository posterRepository;

    @Override
    public void run(String... args) throws Exception {
        List<String> images = List.of(
            "https://bizweb.dktcdn.net/thumb/1024x1024/100/417/212/products/z3491166343428-6e0c0a70b74913e035d60b7157a67963.jpg?v=1710490366577",
            "https://bizweb.dktcdn.net/thumb/1024x1024/100/417/212/products/hat-dieu-aa89e282-c091-4fed-9c55-abc08aaef144.jpg?v=1758811796537",
            "https://vov2.vov.vn/sites/default/files/2024-10/bong-da-nam.jpg"
        );

        List<String> links = List.of(
            "https://iamgvietnam.com/bap-rang-bo-popcorn-vi-phomai-goi-50g",
            "https://iamgvietnam.com/hat-hanh-cot-dua",
            "https://vov2.vov.vn/ban-va-luan/bong-da-viet-nam-ra-bien-lon-con-duong-hoa-hong-hay-chong-gai-50507.vov2"
        );
        
        // Kiểm tra số lượng
        if (images.size() != links.size()) {
            throw new IllegalArgumentException("Số lượng ảnh và link phải bằng nhau!");
        }
        posterRepository.deleteAll(); // xóa bảng poster trước
        // Thêm poster
        for (int i = 0; i < images.size(); i++) {
            Poster poster = new Poster();
            poster.setTen("Poster " + (i + 1));
            poster.setUrl(images.get(i));
            poster.setLinkChuyenHuong(links.get(i));
            posterRepository.save(poster);
        }

        System.out.println("✅ Mock poster đã được thêm!");
    }
}
