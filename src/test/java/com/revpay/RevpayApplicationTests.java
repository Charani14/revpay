package com.revpay;

import com.revpay.consoleui.Main;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import jakarta.annotation.PostConstruct;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class RevpayApplicationTests {

	// Mock the Main bean so its runApp() won't actually run during tests
	@MockBean
	private Main consoleApp;

	// Inject the main application to trigger @PostConstruct
	@Autowired
	private RevpayApplication revpayApplication;

	@Test
	void contextLoadsAndRunAppIsCalled() {
		// Verify that runApp() was called exactly once after context initialized
		verify(consoleApp, times(1)).runApp();
	}
}
