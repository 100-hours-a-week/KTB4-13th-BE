package com.book.core.address.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.application.command.PatchField;
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
    void 전달된_필드만_Command로_변환하고_수정한_주소지_ID를_응답한다() throws Exception {
        final var command = new UpdateAddressCommand(
                42L,
                101L,
                PatchField.of("회사"),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.of(null),
                PatchField.absent());
        when(addressService.updateAddress(command)).thenReturn(new UpdateAddressResult(101L));

        mvc.perform(patch("/api/v1/user-addresses/{addressId}", 101)
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "label": "회사",
                                  "detailAddress": null
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addressId").value(101));

        verify(addressService).updateAddress(command);
    }

    @Test
    void userId나_addressId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(patch("/api/v1/user-addresses/{addressId}", 0)
                        .param("userId", "0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"회사\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void 수정_대상_소유권_오류를_E403으로_응답한다() throws Exception {
        when(addressService.updateAddress(org.mockito.ArgumentMatchers.any(UpdateAddressCommand.class)))
                .thenThrow(new CoreException(ErrorCode.FORBIDDEN));

        mvc.perform(patch("/api/v1/user-addresses/{addressId}", 101)
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"label\":\"회사\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void isDefaultAddress를_전달하면_Command에_포함한다() throws Exception {
        final var command = new UpdateAddressCommand(
                42L,
                101L,
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.absent(),
                PatchField.of(true));
        when(addressService.updateAddress(command)).thenReturn(new UpdateAddressResult(101L));

        mvc.perform(patch("/api/v1/user-addresses/{addressId}", 101)
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isDefaultAddress\":true}"))
                .andExpect(status().isOk());

        verify(addressService).updateAddress(command);
    }

    @Test
    void isDefaultAddress가_null이면_E400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(patch("/api/v1/user-addresses/{addressId}", 101)
                        .param("userId", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isDefaultAddress\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }
}
