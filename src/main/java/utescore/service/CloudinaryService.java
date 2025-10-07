package utescore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadAndGetName(MultipartFile file) throws IOException {
        Map result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("folder", "webnhom20"));
        String name = result.get("public_id") + "." + result.get("format");
        return name;
    }

    public String getImageUrl(String imageName) {
        return "https://res.cloudinary.com/dlh6kxhks/image/upload/" + imageName;
    }
}

