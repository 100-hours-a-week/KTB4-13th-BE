package com.book.core.point.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.point.api.converter.PointCommandConverter;
import com.book.core.point.api.converter.PointResultConverter;
import com.book.core.point.api.response.PointResponse;
import com.book.core.point.api.spec.PointControllerSpec;
import com.book.core.point.application.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/point")
public class PointController implements PointControllerSpec {

    private final PointService pointService;
    private final PointCommandConverter commandConverter;
    private final PointResultConverter resultConverter;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<PointResponse>> getPoint(@UserId final Long userId) {
        final var command = commandConverter.toGetPointCommand(userId);
        final var result = pointService.getPoint(command);
        return ResponseEntity.ok(ApiResponse.ok(resultConverter.toGetPointResponse(result)));
    }
}
