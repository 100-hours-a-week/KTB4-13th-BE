package com.book.core.sample.api;

import com.book.core.sample.api.request.SampleCreateRequest;
import com.book.core.sample.api.response.SampleCreateResponse;
import com.book.core.sample.api.response.SampleQueryResponse;
import com.book.core.sample.api.spec.SampleControllerSpec;
import com.book.core.sample.application.command.SampleQueryCommand;
import com.book.core.sample.application.usecase.SampleCreateUseCase;
import com.book.core.sample.application.usecase.SampleQueryUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/samples")
class SampleController implements SampleControllerSpec {
    private final SampleCreateUseCase createUseCase;
    private final SampleQueryUseCase queryUseCase;

    @PostMapping
    @Override
    public ResponseEntity<SampleCreateResponse> create(@Valid @RequestBody final SampleCreateRequest request) {
        final SampleCreateResponse response = SampleCreateResponse.from(createUseCase.execute(request.toCommand()));
        return ResponseEntity.created(URI.create("/api/v1/samples/" + response.id()))
                .body(response);
    }

    @GetMapping("/{sampleId}")
    @Override
    public SampleQueryResponse query(@PathVariable final Long sampleId) {
        return SampleQueryResponse.from(queryUseCase.execute(new SampleQueryCommand(sampleId)));
    }
}
