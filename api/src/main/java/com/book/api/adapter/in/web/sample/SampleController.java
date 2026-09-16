package com.book.api.adapter.in.web.sample;

import com.book.api.adapter.in.web.sample.request.SampleCreateRequest;
import com.book.api.adapter.in.web.sample.response.SampleCreateResponse;
import com.book.api.adapter.in.web.sample.response.SampleQueryResponse;
import com.book.api.adapter.in.web.sample.spec.SampleControllerSpec;
import com.book.core.application.sample.command.SampleQueryCommand;
import com.book.core.application.sample.port.in.SampleCreateUseCase;
import com.book.core.application.sample.port.in.SampleQueryUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/samples")
class SampleController implements SampleControllerSpec {
    private final SampleCreateUseCase createUseCase;
    private final SampleQueryUseCase queryUseCase;

    SampleController(SampleCreateUseCase createUseCase, SampleQueryUseCase queryUseCase) {
        this.createUseCase = createUseCase;
        this.queryUseCase = queryUseCase;
    }

    @PostMapping
    @Override
    public ResponseEntity<SampleCreateResponse> create(
            @Valid @RequestBody SampleCreateRequest request) {
        SampleCreateResponse response = SampleCreateResponse.from(createUseCase.execute(request.toCommand()));
        return ResponseEntity.created(URI.create("/api/v1/samples/" + response.id())).body(response);
    }

    @GetMapping("/{sampleId}")
    @Override
    public SampleQueryResponse query(@PathVariable Long sampleId) {
        return SampleQueryResponse.from(queryUseCase.execute(new SampleQueryCommand(sampleId)));
    }
}
