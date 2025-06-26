package com.donut_app_backend.donutapp.controller;


import com.donut_app_backend.donutapp.model.*;
import com.donut_app_backend.donutapp.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository orderItemRepo;
    @Autowired private CartRepository cartRepo;
    @Autowired private CartItemRepository cartItemRepo;
    @Autowired private UserRepository userRepo;

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestParam Long userId) {
        try {
            Optional<User> userOpt = userRepo.findById(userId);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Usuario no encontrado");
            }

            Optional<Cart> cartOpt = cartRepo.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
            if (!cartOpt.isPresent()) {
                return ResponseEntity.badRequest().body("No hay carrito activo");
            }

            Cart cart = cartOpt.get();
            List<CartItem> cartItems = cartItemRepo.findByCartId(cart.getId());

            if (cartItems.isEmpty()) {
                return ResponseEntity.badRequest().body("El carrito está vacío");
            }

            // Calcular total
            Double total = cartItems.stream()
                    .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                    .sum();

            // Crear orden
            Order order = new Order(userOpt.get(), cart, total);
            Order savedOrder = orderRepo.save(order);

            // Crear items de la orden
            for (CartItem item : cartItems) {
                OrderItem orderItem = new OrderItem(savedOrder, item.getProduct(),
                        item.getQuantity(), item.getUnitPrice());
                orderItemRepo.save(orderItem);
            }

            // Desactivar carrito
            cart.setActive(false);
            cartRepo.save(cart);

            return ResponseEntity.ok(savedOrder);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar orden: " + e.getMessage());
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserOrders(@RequestParam Long userId) {
        try {
            List<Order> orders = orderRepo.findByUserIdOrderByCreatedAtDesc(userId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener historial de órdenes: " + e.getMessage());
        }
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<?> getOrderItems(@PathVariable Long orderId) {
        try {
            List<OrderItem> items = orderItemRepo.findByOrderId(orderId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener items de la orden: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) {
        try {
            Optional<Order> order = orderRepo.findById(id);
            if (order.isPresent()) {
                return ResponseEntity.ok(order.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener orden: " + e.getMessage());
        }
    }
}