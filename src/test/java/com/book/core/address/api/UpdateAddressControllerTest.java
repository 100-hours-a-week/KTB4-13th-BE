package com.book.core.address.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.application.command.UpdateAddressCommand;
import com.book.core.address.application.result.UpdateAddressResult;
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
class UpdateAddressControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AddressService addressService;

    @Test
    void 전체_주소지_정보를_Command로_변환하고_수정한_주소지_ID를_응답한다() throws Exception {
        final var command = new UpdateAddressCommand(42L, 101L, "회사", "06236", "서울시 강남구", null);
        when(addressService.updateAddress(command)).thenReturn(new UpdateAddressResult(101L));

        mvc.perform(put("/api/v1/user-addresses/{addressId}", 101)
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addressId").value(101));

        verify(addressService).updateAddress(command);
    }

    @Test
    void 전체_주소지_정보가_없으면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(put("/api/v1/user-addresses/{addressId}", 101)
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "회사",
                                  "postalCode": "06236",
                                  "address": "서울시 강남구"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void userId나_addressId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(put("/api/v1/user-addresses/{addressId}", 0)
                        .param("userId", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void 수정_대상_소유권_오류를_E403으로_응답한다() throws Exception {
        when(addressService.updateAddress(org.mockito.ArgumentMatchers.any(UpdateAddressCommand.class)))
                .thenThrow(new CoreException(ErrorCode.FORBIDDEN));

        mvc.perform(put("/api/v1/user-addresses/{addressId}", 101)
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E403"));
    }

    private static String validRequestBody() {
        return """
                {
                  "label": "회사",
                  "postalCode": "06236",
                  "address": "서울시 강남구",
                  "detailAddress": null
                }
                """;
    }
}
