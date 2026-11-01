package com.erp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("foundation")
@AutoConfigureMockMvc
class ErpBackendApplicationTests {

	@Autowired MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void foundationStatusIsAvailable() throws Exception {
		mockMvc.perform(get("/api/v1/system/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phase").value("foundation"));
	}

	@Test
	void otherApiPathsAreNotPublic() throws Exception {
		mockMvc.perform(get("/api/v1/private"))
				.andExpect(status().isUnauthorized());
	}

}
