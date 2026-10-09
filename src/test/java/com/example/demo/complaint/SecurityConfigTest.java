package com.example.demo.complaint;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ComplaintController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"ADMIN_EMAIL=a@b.c", "ADMIN_PASSWORD=pw"})
class SecurityConfigTest {
  @Autowired MockMvc mvc;
  @MockBean ComplaintRepository repo;

  @Test
  void apiNeedsLoginAndAdminCanLogIn() throws Exception {
    mvc.perform(get("/api/complaints")).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/login").param("email", "a@b.c").param("password", "bad"))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/login").param("email", "a@b.c").param("password", "pw"))
        .andExpect(status().isOk());
  }
}
