package utescore.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import utescore.entity.Customer;
import utescore.service.CustomerService;

@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @GetMapping
    public String listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customerPage = customerService.findAll(pageable);
        
        model.addAttribute("customers", customerPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", customerPage.getTotalPages());
        model.addAttribute("totalElements", customerPage.getTotalElements());
        
        return "admin/customers/list";
    }

    @GetMapping("/{id}")
    public String viewCustomer(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return customerService.findById(id)
                .map(customer -> {
                    model.addAttribute("customer", customer);
                    return "admin/customers/view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khách hàng");
                    return "redirect:/admin/customers";
                });
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return customerService.findById(id)
                .map(customer -> {
                    model.addAttribute("customer", customer);
                    model.addAttribute("genders", Customer.Gender.values());
                    return "admin/customers/edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khách hàng");
                    return "redirect:/admin/customers";
                });
    }

    @PostMapping("/{id}/edit")
    public String updateCustomer(@PathVariable Long id, @ModelAttribute Customer customer, RedirectAttributes redirectAttributes) {
        if (!customerService.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khách hàng");
            return "redirect:/admin/customers";
        }
        
        customer.setId(id);
        customerService.save(customer);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin khách hàng thành công");
        return "redirect:/admin/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteCustomer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!customerService.existsById(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khách hàng");
            return "redirect:/admin/customers";
        }
        
        try {
            customerService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khách hàng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa khách hàng do còn dữ liệu liên quan");
        }
        
        return "redirect:/admin/customers";
    }
}