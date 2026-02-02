package com.revpay;

import com.revpay.consoleui.Main;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication(scanBasePackages = {"com.revpay"})
public class RevpayApplication {

	@Autowired
	private Main consoleApp;

	public static void main(String[] args) {
		SpringApplication.run(RevpayApplication.class, args);
	}

	// This method runs after Spring context is ready
	@PostConstruct
	public void startConsoleUI() {
		consoleApp.runApp();
	}
}
