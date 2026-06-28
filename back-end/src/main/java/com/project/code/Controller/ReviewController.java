package com.project.code.Controller;

import com.project.code.Model.Customer;
import com.project.code.Model.Review;
import com.project.code.Repo.CustomerRepository;
import com.project.code.Repo.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/{storeId}/{productId}")
    public Map<String, Object> getReviews(
            @PathVariable Long storeId,
            @PathVariable Long productId) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> filteredReviews = new ArrayList<>();

        for (Review review : reviewRepository.findAll()) {
            if (!storeId.equals(review.getStoreId()) || !productId.equals(review.getProductId())) {
                continue;
            }

            Map<String, Object> reviewData = new HashMap<>();
            reviewData.put("comment", review.getComment());
            reviewData.put("rating", review.getRating());

            Customer customer = customerRepository.findByid(review.getCustomerId());
            reviewData.put("customerName", customer != null ? customer.getName() : "Unknown");

            filteredReviews.add(reviewData);
        }

        response.put("reviews", filteredReviews);
        return response;
    }
}
