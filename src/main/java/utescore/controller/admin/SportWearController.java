package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.entity.SportWear;
import utescore.service.CloudinaryService;
import utescore.service.SportWearService;

@Controller
@RequestMapping("/admin/sportwears")
public class SportWearController {

    @Autowired
    private SportWearService sportWearService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping
    public String listSportWears(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String wearType,
            @RequestParam(required = false) String sizeFilter,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SportWear> sportWearPage;
        
        SportWear.WearType type = null;
        if (wearType != null && !wearType.isEmpty()) {
            try {
                type = SportWear.WearType.valueOf(wearType);
            } catch (IllegalArgumentException e) {
                // Invalid wear type, ignore
            }
        }

        SportWear.Size size_filter = null;
        if (sizeFilter != null && !sizeFilter.isEmpty()) {
            try {
                size_filter = SportWear.Size.valueOf(sizeFilter);
            } catch (IllegalArgumentException e) {
                // Invalid size, ignore
            }
        }
        
        sportWearPage = sportWearService.findByNameContainingAndWearTypeAndSize(name, type, size_filter, pageable);
        
        model.addAttribute("sportWears", sportWearPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", sportWearPage.getTotalPages());
        model.addAttribute("searchName", name);
        model.addAttribute("searchWearType", wearType);
        model.addAttribute("searchSize", sizeFilter);
        model.addAttribute("wearTypes", SportWear.WearType.values());
        model.addAttribute("sizes", SportWear.Size.values());
        return "admin/sportwears/list";
    }
    
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("sportWear", new SportWear());
        model.addAttribute("wearTypes", SportWear.WearType.values());
        model.addAttribute("sizes", SportWear.Size.values());
        return "admin/sportwears/create";
    }

    @PostMapping("/create")
    public String createSportWear(
            @ModelAttribute SportWear sportWear,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        try {
            // Upload image to cloud if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageName = cloudinaryService.uploadAndGetName(imageFile);
                String imageUrl = cloudinaryService.getImageUrl(imageName);
                sportWear.setImageUrl(imageUrl);
            }
            
            sportWearService.save(sportWear);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo trang phục thể thao thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo trang phục: " + e.getMessage());
            return "redirect:/admin/sportwears/create";
        }
        return "redirect:/admin/sportwears";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        SportWear sportWear = sportWearService.findById(id);
        if (sportWear == null) {
            return "redirect:/admin/sportwears";
        }
        model.addAttribute("sportWear", sportWear);
        model.addAttribute("wearTypes", SportWear.WearType.values());
        model.addAttribute("sizes", SportWear.Size.values());
        return "admin/sportwears/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateSportWear(
            @PathVariable Long id,
            @ModelAttribute SportWear sportWear,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        try {
            SportWear existingSportWear = sportWearService.findById(id);
            if (existingSportWear == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy trang phục!");
                return "redirect:/admin/sportwears";
            }
            
            // Upload new image to cloud if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageName = cloudinaryService.uploadAndGetName(imageFile);
                String imageUrl = cloudinaryService.getImageUrl(imageName);
                sportWear.setImageUrl(imageUrl);
            } else {
                // Keep existing image URL if no new image uploaded
                sportWear.setImageUrl(existingSportWear.getImageUrl());
            }
            
            sportWear.setId(id);
            sportWear.setCreatedAt(existingSportWear.getCreatedAt()); // Preserve creation time
            sportWearService.update(sportWear);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trang phục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật trang phục: " + e.getMessage());
        }
        return "redirect:/admin/sportwears";
    }

    @PostMapping("/delete/{id}")
    public String deleteSportWear(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sportWearService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa trang phục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa trang phục này!");
        }
        return "redirect:/admin/sportwears";
    }

    @PostMapping("/toggle-rent-availability/{id}")
    public String toggleRentAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SportWear sportWear = sportWearService.findById(id);
            if (sportWear != null) {
                sportWear.setIsAvailableForRent(!sportWear.getIsAvailableForRent());
                sportWearService.update(sportWear);
                redirectAttributes.addFlashAttribute("successMessage", 
                    sportWear.getIsAvailableForRent() ? "Đã bật cho thuê!" : "Đã tắt cho thuê!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra!");
        }
        return "redirect:/admin/sportwears";
    }

    @PostMapping("/toggle-sale-availability/{id}")
    public String toggleSaleAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SportWear sportWear = sportWearService.findById(id);
            if (sportWear != null) {
                sportWear.setIsAvailableForSale(!sportWear.getIsAvailableForSale());
                sportWearService.update(sportWear);
                redirectAttributes.addFlashAttribute("successMessage", 
                    sportWear.getIsAvailableForSale() ? "Đã bật bán hàng!" : "Đã tắt bán hàng!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra!");
        }
        return "redirect:/admin/sportwears";
    }
}