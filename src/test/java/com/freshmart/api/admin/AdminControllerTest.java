package com.freshmart.api.admin;

import com.freshmart.api.catalog.CategoryRepository;
import com.freshmart.api.catalog.ProductRepository;
import com.freshmart.api.commerce.order.OrderRepository;
import com.freshmart.api.config.SecurityConfig;
import com.freshmart.api.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SecurityConfig.class)
class AdminControllerTest {
 @Autowired MockMvc mvc;
 @MockitoBean ProductRepository products;
 @MockitoBean CategoryRepository categories;
 @MockitoBean OrderRepository orders;
 @MockitoBean UserRepository users;
 @MockitoBean com.freshmart.api.auth.JwtAuthenticationFilter jwtFilter;

 @Test @WithMockUser(roles="ADMIN")
 void adminCanReadDashboard() throws Exception {
  when(products.count()).thenReturn(8L); when(users.countByActiveTrue()).thenReturn(12L);
  when(orders.count()).thenReturn(5L); when(orders.countByStatus("PLACED")).thenReturn(2L);
  when(orders.sumTotals()).thenReturn(new BigDecimal("1250.00"));
  mvc.perform(get("/api/admin/dashboard")).andExpect(status().isOk())
    .andExpect(jsonPath("$.products").value(8)).andExpect(jsonPath("$.pendingOrders").value(2));
 }

 @Test @WithMockUser(roles="CUSTOMER")
 void customerCannotReadAdminDashboard() throws Exception {
  mvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
  verifyNoInteractions(products,orders,users,categories);
 }
}
