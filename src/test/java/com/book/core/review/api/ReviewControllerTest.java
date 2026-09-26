package com.book.core.review.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.api.converter.ReviewCommandConverter;
import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.command.DeleteReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.service.ReviewService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import(ReviewCommandConverter.class)
@ActiveProfiles("test")
class ReviewControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReviewService reviewService;

    @Test
    void 리뷰_작성은_body의_userId와_orderItemId를_Command로_변환한다() throws Exception {
        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":42,\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(reviewService).createReview(new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false));
    }

    @Test
    void 필수_작성값이_없거나_평점이_범위를_벗어나면_Service를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":42,\"targetId\":701,\"rate\":10.1,\"content\":\"본문\",\"isSpoiler\":false}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(reviewService);
    }

    @Test
    void 리뷰_수정은_선택된_필드만_Command에_넣는다() throws Exception {
        mvc.perform(patch("/api/v1/reviews/801").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
            .andExpect(status().isOk());

        verify(reviewService).updateReview(new UpdateReviewCommand(801L, null, "수정", null));
    }

    @Test
    void 리뷰_삭제는_reviewId를_Service에_전달한다() throws Exception {
        mvc.perform(delete("/api/v1/reviews/801")).andExpect(status().isOk());

        verify(reviewService).deleteReview(new DeleteReviewCommand(801L));
    }

    @Test
    void 양수가_아닌_reviewId는_400을_응답한다() throws Exception {
        mvc.perform(delete("/api/v1/reviews/0")).andExpect(status().isBadRequest());

        verifyNoInteractions(reviewService);
    }

    @Test
    void UseCase_오류는_기존_ErrorResponse_HTTP_계약을_따른다() throws Exception {
        doThrow(new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED)).when(reviewService)
            .createReview(new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false));

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":42,\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E7001"));
    }
}
