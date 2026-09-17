package com.book.core.sample.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.sample.application.command.SampleCreateCommand;
import com.book.core.sample.application.command.SampleQueryCommand;
import com.book.core.sample.application.result.SampleCreateResult;
import com.book.core.sample.application.result.SampleQueryResult;
import com.book.core.sample.application.usecase.SampleCreateUseCase;
import com.book.core.sample.application.usecase.SampleQueryUseCase;
import com.book.core.sample.domain.exception.SampleErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(SampleController.class)
@ActiveProfiles("test")
class SampleControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    SampleCreateUseCase createUseCase;

    @MockitoBean
    SampleQueryUseCase queryUseCase;

    @Test
    void 생성_요청을_Command로_변환하고_201과_Location을_응답한다() throws Exception {
        when(createUseCase.execute(new SampleCreateCommand("책"))).thenReturn(new SampleCreateResult(1L, "책"));
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  책  \"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/samples/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("책"));
        verify(createUseCase).execute(new SampleCreateCommand("책"));
    }

    @Test
    void 조회_요청을_Command로_변환하고_200을_응답한다() throws Exception {
        when(queryUseCase.execute(new SampleQueryCommand(42L))).thenReturn(new SampleQueryResult(42L, "책"));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/samples/{sampleId}", 42))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.name").value("책"));
        verify(queryUseCase).execute(new SampleQueryCommand(42L));
    }

    @Test
    void 빈_이름은_400을_응답한다() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(createUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{"})
    void 누락_null_빈문자열_잘못된_JSON을_거부한다(final String body) throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(createUseCase);
    }

    @Test
    void 정규화_후의_이름_길이를_검증한다() throws Exception {
        final String name = "가".repeat(100);
        when(createUseCase.execute(new SampleCreateCommand(name))).thenReturn(new SampleCreateResult(1L, name));
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  " + name + "  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name));
        mvc.perform(MockMvcRequestBuilders.post("/api/v1/samples")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "가\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 존재하지_않는_ID는_404를_응답한다() throws Exception {
        when(queryUseCase.execute(new SampleQueryCommand(42L)))
                .thenThrow(new BusinessException(SampleErrorCode.SAMPLE_NOT_FOUND));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/samples/{sampleId}", 42))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SAMPLE_NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void 잘못된_ID는_400을_응답한다(final String id) throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/samples/{sampleId}", id))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(queryUseCase);
    }

    @Test
    void 저장소_오류의_상세_원인은_HTTP_응답에_노출하지_않는다() throws Exception {
        when(queryUseCase.execute(any()))
                .thenThrow(new BusinessException(
                        CommonErrorCode.STORAGE_FAILURE, new IllegalStateException("internal database detail")));
        mvc.perform(MockMvcRequestBuilders.get("/api/v1/samples/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":\"STORAGE_FAILURE\",\"message\":\"저장소 작업을 완료할 수 없습니다.\"}"));
    }
}
