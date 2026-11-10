package com.book.core.library.infrastructure.client.s3;

import com.book.core.library.application.port.ShelfPhotoStorageClient;
import com.book.core.library.application.result.ShelfPhotoUploadResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/** 형식과 크기를 서명에 포함해 다른 Content-Type·Content-Length로 올리면 S3가 거부하게 합니다. 서명된 주소는 로그에 남기지 않습니다. */
@Component
@RequiredArgsConstructor
class ShelfPhotoStorageClientImpl implements ShelfPhotoStorageClient {
    private static final Duration UPLOAD_EXPIRY = Duration.ofMinutes(5);
    private static final Duration READ_EXPIRY = Duration.ofMinutes(2);

    private final S3Presigner presigner;
    private final ShelfPhotoProperties properties;

    @Override
    public ShelfPhotoUploadResult presignUpload(final String photoKey, final String contentType, final long contentLength) {
        final PresignedPutObjectRequest presigned = presigner
            .presignPutObject(PutObjectPresignRequest.builder().signatureDuration(UPLOAD_EXPIRY).putObjectRequest(PutObjectRequest.builder()
                .bucket(properties.bucket()).key(photoKey).contentType(contentType).contentLength(contentLength).build()).build());
        final Map<String, String> headers = presigned.signedHeaders().entrySet().stream()
            .filter((final Map.Entry<String, List<String>> header) -> !"host".equalsIgnoreCase(header.getKey())).collect(
                Collectors.toMap(Map.Entry::getKey, (final Map.Entry<String, List<String>> header) -> String.join(",", header.getValue())));
        return new ShelfPhotoUploadResult(photoKey, presigned.url().toString(), headers, presigned.expiration());
    }

    @Override
    public String presignRead(final String photoKey) {
        return presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(READ_EXPIRY)
            .getObjectRequest(GetObjectRequest.builder().bucket(properties.bucket()).key(photoKey).build()).build()).url().toString();
    }
}
