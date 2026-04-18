package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.api.dto.OrderResponse;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminDashboardApiController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final ApiMapper apiMapper;

    public AdminDashboardApiController(ProductRepository productRepository,
            OrderService orderService,
            ApiMapper apiMapper) {
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(name = "page", defaultValue = "0") int page) {
        long totalOrders = orderService.countOrders();
        int totalPages = totalOrders == 0 ? 0 : (int) Math.ceil((double) totalOrders / DEFAULT_PAGE_SIZE);
        int safePage = totalPages == 0 ? 0 : Math.max(0, Math.min(page, totalPages - 1));

        return new DashboardResponse(
                productRepository.count(),
                orderService.getTotalItemsSold(),
                totalOrders,
                orderService.getTotalRevenue(),
                safePage,
                totalPages,
                orderService.getRecentOrders(safePage, DEFAULT_PAGE_SIZE).stream().map(apiMapper::toOrderResponse).toList());
    }

    public record DashboardResponse(
            long totalProducts,
            long totalItemsSold,
            long totalOrders,
            double totalRevenue,
            int currentPage,
            int totalPages,
            List<OrderResponse> recentOrders) {
    }
}
