package utescore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    // Upload từ MultipartFile (giữ nguyên nếu dùng form upload bình thường)
    public String uploadAndGetName(MultipartFile file) throws IOException {
        Map result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", "webnhom20"));
        return result.get("public_id") + "." + result.get("format");
    }

    // Upload trực tiếp từ File
    public String uploadAndGetName(File file) throws IOException {
        Map result = cloudinary.uploader().upload(file, ObjectUtils.asMap("folder", "webnhom20"));
        return result.get("public_id") + "." + result.get("format");
    }

    public String getImageUrl(String imageName) {
        return "https://res.cloudinary.com/dlh6kxhks/image/upload/" + imageName;
    }
}


