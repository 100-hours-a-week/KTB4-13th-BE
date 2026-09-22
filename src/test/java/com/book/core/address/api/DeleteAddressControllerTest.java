package com.book.core.address.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.application.command.DeleteAddressCommand;
import com.book.core.address.application.service.AddressService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AddressController.class)
@Import({AddressCommandConverter.class, AddressResultConverter.class})
@ActiveProfiles("test")
class DeleteAddressControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AddressService addressService;

    @Test
    void 배송지를_삭제하면_빈_본문으로_200을_응답한다() throws Exception {
        mvc.perform(delete("/api/v1/user-addresses/{addressId}", 101).param("userId", "42"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(addressService).deleteAddress(new DeleteAddressCommand(42L, 101L));
    }

    @Test
    void userId나_addressId가_양수가_아니면_400을_응답하고_Service를_호출하지_않는다() throws Exception {
        mvc.perform(delete("/api/v1/user-addresses/{addressId}", 0).param("userId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(addressService);
    }

    @Test
    void 삭제_대상_소유권_오류를_E403으로_응답한다() throws Exception {
        doThrow(new CoreException(ErrorCode.FORBIDDEN))
                .when(addressService)
                .deleteAddress(org.mockito.ArgumentMatchers.any(DeleteAddressCommand.class));

        mvc.perform(delete("/api/v1/user-addresses/{addressId}", 101).param("userId", "42"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("E403"));
    }
}
