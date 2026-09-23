package com.ebank;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class EBankApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	private org.springframework.security.authentication.AuthenticationManager authenticationManager;

	@org.springframework.beans.factory.annotation.Autowired
	private com.ebank.repository.UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void testAdminLogin() {
		var users = userRepository.findAll();
		System.out.println("Users in db count: " + users.size());
		for (var u : users) {
			System.out.println("User: " + u.getUsername() + ", passHash=" + u.getPasswordHash() + ", status=" + u.getStatus());
		}
		var auth = authenticationManager.authenticate(
				new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin", "admin123")
		);
		org.junit.jupiter.api.Assertions.assertNotNull(auth);
		org.junit.jupiter.api.Assertions.assertTrue(auth.isAuthenticated());
	}
}
