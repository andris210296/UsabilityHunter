package br.com.uhunter.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class UsabilityHunterApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void runTestRequiresUrl() throws Exception {
		mockMvc.perform(get("/uhunter/runtests"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void runTestRejectsBlankUrl() throws Exception {
		mockMvc.perform(get("/uhunter/runtests").param("url", " "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").exists());
	}
}
