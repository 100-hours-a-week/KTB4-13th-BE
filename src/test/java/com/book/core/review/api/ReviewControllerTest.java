package com.book.core.review.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.review.api.converter.ReviewCommandConverter;
import com.book.core.review.api.converter.ReviewResultConverter;
import com.book.common.paging.OffsetLimit;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.ReviewContent;
import com.book.common.paging.Page;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.util.List;
import com.book.core.review.application.command.AddReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.RemoveReviewCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.service.ReviewService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import({ReviewCommandConverter.class, ReviewResultConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class ReviewControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    ReviewService reviewService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 회원은_리뷰를_생성할_수_있다() throws Exception {
        authenticateAs(42L);
        when(reviewService.addReview(new AddReviewCommand(42L, new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.5"), "본문", false)))).thenReturn(801L);

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":999,\"targetType\":\"PRODUCT\",\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").value(801));

        verify(reviewService).addReview(new AddReviewCommand(42L, new ReviewTarget(ReviewTargetType.PRODUCT, 701L),
            new ReviewContent(new BigDecimal("4.5"), "본문", false)));
    }

    @Test
    void 회원은_유효하지_않은_요청값으로_리뷰를_생성할_수_없다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetType\":\"PRODUCT\",\"targetId\":701,\"rate\":10.1,\"content\":\"본문\",\"isSpoiler\":false}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetType\":\"PRODUCT\",\"targetId\":701,\"rate\":4.1,\"content\":\"본문\",\"isSpoiler\":false}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetType\":\"PRODUCT\",\"targetId\":701,\"rate\":4.5,\"content\":\"   \",\"isSpoiler\":false}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewService);
    }

    @Test
    void 회원은_자신의_리뷰를_수정할_수_있다() throws Exception {
        authenticateAs(42L);
        when(reviewService.updateReview(new UpdateReviewCommand(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정", null))))
            .thenReturn(801L);

        mvc.perform(patch("/api/v1/reviews/801").contentType(MediaType.APPLICATION_JSON).content("{\"rate\":4.5,\"content\":\"수정\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data").value(801));

        verify(reviewService).updateReview(new UpdateReviewCommand(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정", null)));
    }

    @Test
    void 회원은_다른_회원의_리뷰를_수정할_수_없다() throws Exception {
        authenticateAs(999L);
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(reviewService)
            .updateReview(new UpdateReviewCommand(999L, 801L, new ReviewContent(new BigDecimal("4.5"), "수정", null)));

        mvc.perform(patch("/api/v1/reviews/801").contentType(MediaType.APPLICATION_JSON).content("{\"rate\":4.5,\"content\":\"수정\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void 회원은_필수_수정_요청값이_없으면_리뷰를_수정할_수_없다() throws Exception {
        authenticateAs(42L);
        mvc.perform(patch("/api/v1/reviews/801").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/reviews/801").contentType(MediaType.APPLICATION_JSON).content("{\"rate\":4.5}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewService);
    }

    @Test
    void 회원은_자신의_리뷰를_삭제할_수_있다() throws Exception {
        authenticateAs(42L);

        when(reviewService.removeReview(new RemoveReviewCommand(42L, 801L))).thenReturn(801L);

        mvc.perform(delete("/api/v1/reviews/801")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").value(801));

        verify(reviewService).removeReview(new RemoveReviewCommand(42L, 801L));
    }

    @Test
    void 회원은_다른_회원의_리뷰를_삭제할_수_없다() throws Exception {
        authenticateAs(999L);
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(reviewService).removeReview(new RemoveReviewCommand(999L, 801L));

        mvc.perform(delete("/api/v1/reviews/801")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void 회원은_양수가_아닌_리뷰_ID로_리뷰를_삭제할_수_없다() throws Exception {
        authenticateAs(42L);

        mvc.perform(delete("/api/v1/reviews/0")).andExpect(status().isBadRequest());

        verifyNoInteractions(reviewService);
    }

    @Test
    void 리뷰_작성에_실패하면_업무_오류를_HTTP_오류_응답으로_반환한다() throws Exception {
        authenticateAs(42L);
        doThrow(new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED)).when(reviewService).addReview(new AddReviewCommand(42L,
            new ReviewTarget(ReviewTargetType.PRODUCT, 701L), new ReviewContent(new BigDecimal("4.5"), "본문", false)));

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetType\":\"PRODUCT\",\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E7001"));
    }

    @Test
    void 회원은_대상별_리뷰_페이지를_조회할_수_있다() throws Exception {
        final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
        final OffsetLimit paging = new OffsetLimit(3, 2);
        final ReviewItemResult item =
            new ReviewItemResult(8L, new ReviewTarget(ReviewTargetType.PRODUCT, 10L), new BigDecimal("4.5"), "본문", true, 42L);
        when(reviewService.findReviews(new GetReviewsCommand(target, paging))).thenReturn(new Page<>(List.of(item), true));

        mvc.perform(get("/api/v1/reviews").param("targetType", "PRODUCT").param("targetId", "10").param("offset", "3").param("limit", "2"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].id").value(8))
            .andExpect(jsonPath("$.data.content[0].targetType").value("PRODUCT"))
            .andExpect(jsonPath("$.data.content[0].targetId").value(10)).andExpect(jsonPath("$.data.content[0].rate").value(4.5))
            .andExpect(jsonPath("$.data.content[0].isSpoiler").value(true)).andExpect(jsonPath("$.data.content[0].content").value("본문"))
            .andExpect(jsonPath("$.data.content[0].isMyReview").value(false)).andExpect(jsonPath("$.data.content[0].userId").doesNotExist())
            .andExpect(jsonPath("$.data.hasNext").value(true));

        verify(reviewService).findReviews(new GetReviewsCommand(target, paging));
    }

    @Test
    void 리뷰가_없으면_회원에게_빈_페이지를_반환한다() throws Exception {
        final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
        final OffsetLimit paging = new OffsetLimit(0, 20);
        when(reviewService.findReviews(new GetReviewsCommand(target, paging))).thenReturn(new Page<>(List.of(), false));

        mvc.perform(get("/api/v1/reviews").param("targetType", "PRODUCT").param("targetId", "10")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isEmpty()).andExpect(jsonPath("$.data.hasNext").value(false));
    }

    @Test
    void 회원은_유효하지_않은_조회_요청으로_리뷰를_조회할_수_없다() throws Exception {
        mvc.perform(get("/api/v1/reviews").param("targetType", "UNKNOWN").param("targetId", "10")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/reviews").param("targetType", "PRODUCT").param("targetId", "0")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/reviews").param("targetType", "PRODUCT").param("targetId", "10").param("offset", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/reviews").param("targetType", "PRODUCT").param("targetId", "10").param("limit", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/reviews").param("targetType", "PRODUCT").param("targetId", "10").param("limit", "101"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(reviewService);
    }

    @Test
    void 인증_회원은_본인과_타인의_리뷰를_구분하며_요청한_회원_ID는_무시한다() throws Exception {
        authenticateAs(42L);
        final ReviewTarget target = new ReviewTarget(ReviewTargetType.PRODUCT, 10L);
        final ReviewItemResult own = new ReviewItemResult(8L, target, new BigDecimal("4.5"), "본인", false, 42L);
        final ReviewItemResult other = new ReviewItemResult(9L, target, new BigDecimal("4.0"), "타인", true, 99L);
        when(reviewService.findReviews(new GetReviewsCommand(target, new OffsetLimit(0, 20))))
            .thenReturn(new Page<>(List.of(own, other), false));

        mvc.perform(get("/api/v1/reviews").param("targetType", "PRODUCT").param("targetId", "10").param("userId", "99"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.content[0].isMyReview").value(true))
            .andExpect(jsonPath("$.data.content[1].isMyReview").value(false))
            .andExpect(jsonPath("$.data.content[0].userId").doesNotExist());
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }
}
