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
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    @Autowired private CartRepository cartRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private CartItemRepository cartItemRepo;

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Long userId,
                                       @RequestParam Long productId,
                                       @RequestParam Integer quantity) {
        try {
            Optional<User> userOpt = userRepo.findById(userId);
            Optional<Product> productOpt = productRepo.findById(productId);

            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Usuario no encontrado");
            }
            if (!productOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Producto no encontrado");
            }

            User user = userOpt.get();
            Product product = productOpt.get();

            if (!product.getAvailable()) {
                return ResponseEntity.badRequest().body("Producto no disponible");
            }

            // Buscar carrito activo o crear uno nuevo
            Optional<Cart> cartOpt = cartRepo.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
            Cart cart;

            if (cartOpt.isPresent()) {
                cart = cartOpt.get();
            } else {
                cart = new Cart(user);
                cart = cartRepo.save(cart);
            }

            // Verificar si el producto ya está en el carrito
            Optional<CartItem> existingItem = cartItemRepo.findByCartIdAndProductId(cart.getId(), productId);

            if (existingItem.isPresent()) {
                // Actualizar cantidad
                CartItem item = existingItem.get();
                item.setQuantity(item.getQuantity() + quantity);
                cartItemRepo.save(item);
                return ResponseEntity.ok(item);
            } else {
                // Crear nuevo item
                CartItem newItem = new CartItem(cart, product, quantity, product.getPrice());
                cartItemRepo.save(newItem);
                return ResponseEntity.ok(newItem);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al agregar al carrito: " + e.getMessage());
        }
    }

    @GetMapping("/view")
    public ResponseEntity<?> viewCart(@RequestParam Long userId) {
        try {
            Optional<Cart> cartOpt = cartRepo.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);

            if (cartOpt.isPresent()) {
                List<CartItem> items = cartItemRepo.findByCartId(cartOpt.get().getId());
                return ResponseEntity.ok(items);
            } else {
                return ResponseEntity.ok(List.of()); // Carrito vacío
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener carrito: " + e.getMessage());
        }
    }

    @DeleteMapping("/remove/{itemId}")
    public ResponseEntity<?> removeFromCart(@PathVariable Long itemId) {
        try {
            if (cartItemRepo.existsById(itemId)) {
                cartItemRepo.deleteById(itemId);
                return ResponseEntity.ok("Item eliminado del carrito");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar item del carrito: " + e.getMessage());
        }
    }

    @PutMapping("/update/{itemId}")
    public ResponseEntity<?> updateCartItem(@PathVariable Long itemId, @RequestParam Integer quantity) {
        try {
            Optional<CartItem> itemOpt = cartItemRepo.findById(itemId);

            if (itemOpt.isPresent()) {
                CartItem item = itemOpt.get();
                item.setQuantity(quantity);
                cartItemRepo.save(item);
                return ResponseEntity.ok(item);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al actualizar item del carrito: " + e.getMessage());
        }
    }
}


