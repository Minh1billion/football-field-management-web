package utescore.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import utescore.service.CloudinaryService;

@RestController
@RequestMapping("/api/images")
public class UploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            return cloudinaryService.uploadAndGetName(file);
        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }

    @GetMapping("/{name}")
    public String getImageUrl(@PathVariable String name) {
        return cloudinaryService.getImageUrl(name);
    }
}