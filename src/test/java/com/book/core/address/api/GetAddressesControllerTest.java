package com.book.core.address.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.address.api.converter.AddressCommandConverter;
import com.book.core.address.api.converter.AddressResultConverter;
import com.book.core.address.application.command.GetAddressesCommand;
import com.book.core.address.application.result.GetAddressItemResult;
import com.book.core.address.application.result.GetAddressesResult;
import com.book.core.address.application.service.AddressService;
import java.util.List;
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
class GetAddressesControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AddressService addressService;

    @Test
    void 인증_회원의_주소지_목록을_명세_필드명으로_응답한다() throws Exception {
        final GetAddressesCommand command = new GetAddressesCommand(42L);
        when(addressService.getAddresses(command))
                .thenReturn(new GetAddressesResult(
                        List.of(new GetAddressItemResult(101L, "집", "06236", "서울특별시 강남구 테헤란로 1", "101호", true))));

        mvc.perform(get("/api/v1/user-addresses").param("userId", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addresses[0].addressid").value(101))
                .andExpect(jsonPath("$.data.addresses[0].addressId").doesNotExist())
                .andExpect(jsonPath("$.data.addresses[0].addressLabel").value("집"))
                .andExpect(jsonPath("$.data.addresses[0].addressPostalCode").value("06236"))
                .andExpect(jsonPath("$.data.addresses[0].address").value("서울특별시 강남구 테헤란로 1"))
                .andExpect(jsonPath("$.data.addresses[0].detailAddress").value("101호"))
                .andExpect(jsonPath("$.data.addresses[0].isDefault").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isEmpty());

        verify(addressService).getAddresses(command);
    }

    @Test
    void 저장소_오류는_E500으로_응답한다() throws Exception {
        when(addressService.getAddresses(any(GetAddressesCommand.class)))
                .thenThrow(new IllegalStateException("database detail"));

        mvc.perform(get("/api/v1/user-addresses").param("userId", "42"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.message").value("알 수 없는 오류가 발생했습니다."));
    }
}
