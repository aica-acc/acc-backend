package com.assistant.acc.controller.banner;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BannerHelloController {

    @GetMapping("/banner")
    public String hello() {
        return "HelloBanner";
    }
}
