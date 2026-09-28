package com.book.core.review.api;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(ReviewController.class)
@Import({ReviewCommandConverter.class, ReviewControllerTest.AuthenticationPrincipalTestConfig.class})
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
    void 리뷰_작성은_인증된_userId와_요청_targetId를_Command로_변환한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(reviewService).createReview(new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false));
    }

    @Test
    void 요청_body에_userId를_전달해도_무시되고_인증된_userId가_사용된다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"userId\":999,\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isOk());

        verify(reviewService).createReview(new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false));
    }

    @Test
    void 필수_작성값이_없거나_평점이_범위를_벗어나면_Service를_호출하지_않는다() throws Exception {
        authenticateAs(42L);

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetId\":701,\"rate\":10.1,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(reviewService);
    }

    @Test
    void 리뷰_수정은_인증된_userId와_reviewId를_Command로_전달한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(patch("/api/v1/reviews/801").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
            .andExpect(status().isOk());

        verify(reviewService).updateReview(new UpdateReviewCommand(42L, 801L, null, "수정", null));
    }

    @Test
    void 다른_사용자의_리뷰_수정_시도는_403을_응답한다() throws Exception {
        authenticateAs(999L);
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(reviewService)
            .updateReview(new UpdateReviewCommand(999L, 801L, null, "수정", null));

        mvc.perform(patch("/api/v1/reviews/801").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void 리뷰_삭제는_인증된_userId와_reviewId를_Service에_전달한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(delete("/api/v1/reviews/801")).andExpect(status().isOk());

        verify(reviewService).deleteReview(new DeleteReviewCommand(42L, 801L));
    }

    @Test
    void 다른_사용자의_리뷰_삭제_시도는_403을_응답한다() throws Exception {
        authenticateAs(999L);
        doThrow(new CoreException(ErrorCode.FORBIDDEN)).when(reviewService).deleteReview(new DeleteReviewCommand(999L, 801L));

        mvc.perform(delete("/api/v1/reviews/801")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("E403"));
    }

    @Test
    void 양수가_아닌_reviewId는_400을_응답한다() throws Exception {
        authenticateAs(42L);

        mvc.perform(delete("/api/v1/reviews/0")).andExpect(status().isBadRequest());

        verifyNoInteractions(reviewService);
    }

    @Test
    void UseCase_오류는_기존_ErrorResponse_HTTP_계약을_따른다() throws Exception {
        authenticateAs(42L);
        doThrow(new CoreException(ErrorCode.REVIEW_ALREADY_REVIEWED)).when(reviewService)
            .createReview(new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "본문", false));

        mvc.perform(post("/api/v1/reviews").contentType(MediaType.APPLICATION_JSON)
            .content("{\"targetId\":701,\"rate\":4.5,\"content\":\"본문\",\"isSpoiler\":false}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("E7001"));
    }

    private void authenticateAs(final Long userId) {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", userId.toString()).issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600)).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
