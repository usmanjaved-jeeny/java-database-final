package com.project.code.Controller;

import com.project.code.Model.CombinedRequest;
import com.project.code.Model.Inventory;
import com.project.code.Model.Product;
import com.project.code.Repo.InventoryRepository;
import com.project.code.Repo.ProductRepository;
import com.project.code.Service.ServiceClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ServiceClass serviceClass;

    @PutMapping
    public Map<String, String> updateInventory(@RequestBody CombinedRequest combinedRequest) {
        Map<String, String> response = new HashMap<>();

        try {
            Product product = combinedRequest.getProduct();
            if (!serviceClass.ValidateProductId(product.getId())) {
                response.put("message", "Product not present in database");
                return response;
            }

            productRepository.save(product);

            Inventory existingInventory = serviceClass.getInventoryId(combinedRequest.getInventory());
            if (existingInventory != null) {
                existingInventory.setStockLevel(combinedRequest.getInventory().getStockLevel());
                inventoryRepository.save(existingInventory);
                response.put("message", "Successfully updated product");
            } else {
                response.put("message", "No data available");
            }
        } catch (DataIntegrityViolationException e) {
            response.put("message", "Error updating inventory: " + e.getMessage());
        }

        return response;
    }

    @PostMapping
    public Map<String, String> saveInventory(@RequestBody Inventory inventory) {
        Map<String, String> response = new HashMap<>();

        try {
            if (!serviceClass.validateInventory(inventory)) {
                response.put("message", "Data is already present");
                return response;
            }

            inventoryRepository.save(inventory);
            response.put("message", "Data saved successfully");
        } catch (DataIntegrityViolationException e) {
            response.put("message", "Error saving inventory: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/{storeid}")
    public Map<String, Object> getAllProducts(@PathVariable Long storeid) {
        Map<String, Object> response = new HashMap<>();
        List<Product> products = productRepository.findProductsByStoreId(storeid);
        response.put("products", products);
        return response;
    }

    @GetMapping("/filter/{category}/{name}/{storeId}")
    public ResponseEntity<Map<String, Object>> getProductName(
            @PathVariable String category,
            @PathVariable String name,
            @PathVariable Long storeId) {
        Map<String, Object> response = new HashMap<>();

        if (storeId == null || storeId <= 0) {
            response.put("message", "Invalid store ID");
            response.put("product", Collections.emptyList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        if (category == null || name == null) {
            response.put("message", "Category and name parameters are required");
            response.put("product", Collections.emptyList());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        List<Product> products;

        if (category.equals("null")) {
            products = productRepository.findByNameLike(storeId, name);
        } else if (name.equals("null")) {
            products = productRepository.findByCategoryAndStoreId(storeId, category);
        } else {
            products = productRepository.findByNameAndCategory(storeId, name, category);
        }

        response.put("product", products);
        return ResponseEntity.ok(response);
    }

    @GetMapping("search/{name}/{storeId}")
    public Map<String, Object> searchProduct(
            @PathVariable String name,
            @PathVariable Long storeId) {
        Map<String, Object> response = new HashMap<>();
        List<Product> products = productRepository.findByNameLike(storeId, name);
        response.put("product", products);
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, String> removeProduct(@PathVariable Long id) {
        Map<String, String> response = new HashMap<>();

        if (!serviceClass.ValidateProductId(id)) {
            response.put("message", "Product not present in database");
            return response;
        }

        inventoryRepository.deleteByProductId(id);
        response.put("message", "Product deleted successfully");
        return response;
    }

    @GetMapping("/validate/{quantity}/{storeId}/{productId}")
    public ResponseEntity<Map<String, Object>> validateQuantity(
            @PathVariable Integer quantity,
            @PathVariable Long storeId,
            @PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();

        if (quantity == null || quantity <= 0 || storeId == null || storeId <= 0 || productId == null || productId <= 0) {
            response.put("valid", false);
            response.put("message", "Invalid input parameters");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Inventory inventory = inventoryRepository.findByProductIdandStoreId(productId, storeId);
        if (inventory == null) {
            response.put("valid", false);
            response.put("message", "Inventory not found for the given product and store");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        boolean sufficientStock = inventory.getStockLevel() >= quantity;
        response.put("valid", sufficientStock);
        response.put("message", sufficientStock
                ? "Sufficient stock available"
                : "Insufficient stock available");
        response.put("availableQuantity", inventory.getStockLevel());

        return ResponseEntity.ok(response);
    }
}
