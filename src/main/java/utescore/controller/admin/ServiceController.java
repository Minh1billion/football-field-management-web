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
import utescore.entity.Service;
import utescore.service.CloudinaryService;
import utescore.service.ServiceService;

@Controller
@RequestMapping("/admin/services")
public class ServiceController {

    @Autowired
    private ServiceService serviceService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping
    public String listServices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String serviceType,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Service> servicePage;
        
        Service.ServiceType type = null;
        if (serviceType != null && !serviceType.isEmpty()) {
            try {
                type = Service.ServiceType.valueOf(serviceType);
            } catch (IllegalArgumentException e) {
                // Ignore invalid service type
            }
        }
        
        servicePage = serviceService.findByNameContainingAndServiceType(name, type, pageable);
        
        model.addAttribute("services", servicePage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", servicePage.getTotalPages());
        model.addAttribute("searchName", name);
        model.addAttribute("searchServiceType", serviceType);
        model.addAttribute("serviceTypes", Service.ServiceType.values());
        return "admin/services/list";
    }
    
    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("service", new Service());
        model.addAttribute("serviceTypes", Service.ServiceType.values());
        return "admin/services/create";
    }

    @PostMapping("/create")
    public String createService(
            @ModelAttribute Service service,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        try {
            // Upload image to cloud if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageName = cloudinaryService.uploadAndGetName(imageFile);
                String imageUrl = cloudinaryService.getImageUrl(imageName);
                service.setImageUrl(imageUrl);
            }
            
            serviceService.save(service);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi tạo dịch vụ: " + e.getMessage());
            return "redirect:/admin/services/create";
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Service service = serviceService.findById(id);
        if (service == null) {
            return "redirect:/admin/services";
        }
        model.addAttribute("service", service);
        model.addAttribute("serviceTypes", Service.ServiceType.values());
        return "admin/services/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateService(
            @PathVariable Long id,
            @ModelAttribute Service service,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {
        try {
            Service existingService = serviceService.findById(id);
            if (existingService == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy dịch vụ!");
                return "redirect:/admin/services";
            }
            
            // Upload new image to cloud if provided
            if (imageFile != null && !imageFile.isEmpty()) {
                String imageName = cloudinaryService.uploadAndGetName(imageFile);
                String imageUrl = cloudinaryService.getImageUrl(imageName);
                service.setImageUrl(imageUrl);
            } else {
                // Keep existing image URL if no new image uploaded
                service.setImageUrl(existingService.getImageUrl());
            }
            
            service.setId(id);
            service.setCreatedAt(existingService.getCreatedAt()); // Preserve creation time
            serviceService.update(service);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra khi cập nhật dịch vụ: " + e.getMessage());
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/delete/{id}")
    public String deleteService(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            serviceService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa dịch vụ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa dịch vụ này!");
        }
        return "redirect:/admin/services";
    }

    @PostMapping("/toggle-availability/{id}")
    public String toggleAvailability(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Service service = serviceService.findById(id);
            if (service != null) {
                service.setIsAvailable(!service.getIsAvailable());
                serviceService.update(service);
                redirectAttributes.addFlashAttribute("successMessage", 
                    service.getIsAvailable() ? "Đã bật dịch vụ!" : "Đã tắt dịch vụ!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra!");
        }
        return "redirect:/admin/services";
    }

    @GetMapping("/view/{id}")
    public String viewService(@PathVariable Long id, Model model) {
        Service service = serviceService.findById(id);
        if (service == null) {
            return "redirect:/admin/services";
        }
        model.addAttribute("service", service);
        return "admin/services/view";
    }
}