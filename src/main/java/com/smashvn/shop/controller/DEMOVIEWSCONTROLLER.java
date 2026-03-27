package com.smashvn.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DEMOVIEWSCONTROLLER {
	@RequestMapping("/")
	public String DemoViews() {
		return "index";
	}
}
