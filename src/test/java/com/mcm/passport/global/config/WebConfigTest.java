package com.mcm.passport.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:web-config;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class WebConfigTest {

	@Autowired
	private WebApplicationContext applicationContext;

	@Test
	void allowsFrontendPreflightRequestsForApiEndpoints() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();

		mockMvc.perform(options("/api/passport-sessions")
					.header("Origin", "http://localhost:3000")
					.header("Access-Control-Request-Method", "POST")
					.header("Access-Control-Request-Headers", "Content-Type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
				.andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"));
	}
}
