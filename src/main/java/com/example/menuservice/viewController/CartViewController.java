package com.example.menuservice.viewController;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartViewController {

    @GetMapping("/cart")
    public String viewCart() {
        return "cartList"; // cartList.html
    }
}


