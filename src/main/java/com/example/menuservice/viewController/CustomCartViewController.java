package com.example.menuservice.viewController;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class CustomCartViewController {

    @GetMapping("/customCart")
    public String showCustomCartForm() {
        return "customCart";  // 재료 없이 뷰만 반환
    }
}
