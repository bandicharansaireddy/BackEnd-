package com.freshmart.api.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CatalogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(com.freshmart.api.config.SecurityConfig.class)
class CatalogControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private ProductRepository products;
    @MockitoBean private CategoryRepository categories;
    @MockitoBean private com.freshmart.api.auth.JwtAuthenticationFilter jwtFilter;

    @Test
    void searchPassesValidatedFiltersToRepository() throws Exception {
        when(products.search(eq("fruit"), eq("mango"), eq(new BigDecimal("20.00")),
            eq(new BigDecimal("200.00")), any(Pageable.class))).thenReturn(Page.empty());

        mvc.perform(get("/api/products")
                .param("q", "mango")
                .param("category", "fruit")
                .param("minPrice", "20.00")
                .param("maxPrice", "200.00")
                .param("page", "0")
                .param("size", "12")
                .param("sort", "price-asc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(0));

        verify(products).search(eq("fruit"), eq("mango"), eq(new BigDecimal("20.00")),
            eq(new BigDecimal("200.00")), any(Pageable.class));
    }

    @Test
    void rejectsInvalidPriceRange() throws Exception {
        mvc.perform(get("/api/products").param("minPrice", "300").param("maxPrice", "10"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(products);
    }

    @Test
    void rejectsUnboundedPageSizes() throws Exception {
        mvc.perform(get("/api/products").param("size", "101"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(products);
    }
}
