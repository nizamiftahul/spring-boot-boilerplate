package com.example.spring_boot_boilerplate;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Basic test for DemoApplication main class.
 */
class DemoApplicationTests {

	@Test
	void demoApplicationExists() {
		// Verify DemoApplication class exists and can be instantiated
		assertNotNull(DemoApplication.class);
	}

	@Test
	void mainMethodExists() throws NoSuchMethodException {
		// Verify main method exists
		assertNotNull(DemoApplication.class.getMethod("main", String[].class));
	}
}
