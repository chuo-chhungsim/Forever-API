package com.forever.foreverapi.modules.admin.controller;

import com.forever.foreverapi.base.PageResponse;
import com.forever.foreverapi.modules.product.dto.ProductDtos.ProductForm;
import com.forever.foreverapi.modules.product.dto.ProductDtos.ProductResponse;
import com.forever.foreverapi.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Admin - Products", description = "ADMIN role only. Images are uploaded to MinIO.")
public class AdminProductController {

    private final ProductService products;

    public AdminProductController(ProductService products) {
        this.products = products;
    }

    @Operation(summary = "List all products including deactivated ones")
    @GetMapping
    public PageResponse<ProductResponse> list(@RequestParam(required = false) String q,
                                              @RequestParam(required = false) List<String> category,
                                              @RequestParam(required = false) List<String> subCategory,
                                              @RequestParam(defaultValue = "newest") String sort,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return products.search(q, category, subCategory, sort, page, size, true);
    }

    @Operation(summary = "One product, including hidden ones")
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return products.getForAdmin(id);
    }

    @Operation(summary = "Create a product (multipart/form-data, 1-4 images)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @ModelAttribute ProductForm form) {
        return products.create(form);
    }

    @Operation(summary = "Update a product; sending images replaces the existing ones")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse update(@PathVariable Long id, @Valid @ModelAttribute ProductForm form) {
        return products.update(id, form);
    }

    @Operation(summary = "Show/hide a product in the shop")
    @PatchMapping("/{id}/active")
    public ProductResponse setActive(@PathVariable Long id, @RequestParam boolean active) {
        return products.setActive(id, active);
    }

    @Operation(summary = "Remove a product from the shop (soft delete, existing orders keep their snapshot)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        products.delete(id);
    }
}
