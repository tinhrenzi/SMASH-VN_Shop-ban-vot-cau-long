package com.smashvn.shop.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class AdminControllerRenderTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    public void testDonHangRender() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        CsrfToken csrfToken = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "mock-token-value");
        
        mockMvc.perform(get("/admin/don-hang")
                .requestAttr("_csrf", csrfToken)
                .sessionAttr("vaiTro", "QL")
                .sessionAttr("laKhachHang", false)
                .sessionAttr("laNhanVien", false)
                .sessionAttr("laQuanLy", true))
                .andExpect(status().isOk())
                .andDo(print());
    }
}
