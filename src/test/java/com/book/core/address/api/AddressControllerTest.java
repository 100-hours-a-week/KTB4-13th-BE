package com.book.core.address.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.application.command.RegisterAddressCommand;
import com.book.core.address.application.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AddressController.class)
@Import({AddressCommandConverter.class, AddressResultConverter.class})
@ActiveProfiles("test")
class AddressControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AddressService addressService;

    @Test
    void 주소지를_등록하면_성공_응답을_반환한다() throws Exception {
        mvc.perform(post("/api/v1/user-addresses")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "detailAddress": "101호",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(addressService).registerAddress(new RegisterAddressCommand(42L, "집", "12345", "서울시 강남구", "101호", false));
    }

    @Test
    void isDefault가_누락되면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/user-addresses")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void isDefault가_false면_false로_Command를_전달한다() throws Exception {
        mvc.perform(post("/api/v1/user-addresses")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isOk());

        verify(addressService).registerAddress(new RegisterAddressCommand(42L, "집", "12345", "서울시 강남구", null, false));
    }

    @Test
    void 필수_주소지_값이_공백이면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/user-addresses")
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "   ",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void userId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/user-addresses")
                        .param("userId", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "집",
                                  "postalCode": "12345",
                                  "address": "서울시 강남구",
                                  "isDefault": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }
}
