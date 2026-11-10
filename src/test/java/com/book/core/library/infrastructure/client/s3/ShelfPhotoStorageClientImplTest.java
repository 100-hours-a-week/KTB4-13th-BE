package com.book.core.library.infrastructure.client.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import com.book.core.library.application.result.ShelfPhotoUploadResult;
import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class ShelfPhotoStorageClientImplTest {
    private static final String PHOTO_KEY = "shelf-photos/42/0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d.jpg";

    private final S3Presigner presigner = S3Presigner.builder().region(Region.AP_NORTHEAST_2)
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("AKIATESTKEY", "test-secret"))).build();
    private final ShelfPhotoStorageClientImpl client =
        new ShelfPhotoStorageClientImpl(presigner, new ShelfPhotoProperties("book-photos", "ap-northeast-2"));

    @Test
    void 업로드_주소는_5분_동안_형식과_크기를_서명해_다른_파일을_올리면_S3가_거부하게_한다() {
        final Instant before = Instant.now();

        final ShelfPhotoUploadResult upload = client.presignUpload(PHOTO_KEY, "image/jpeg", 1_048_576L);

        assertThat(upload.photoKey()).isEqualTo(PHOTO_KEY);
        final URI url = URI.create(upload.uploadUrl());
        assertThat(url.getHost()).isEqualTo("book-photos.s3.ap-northeast-2.amazonaws.com");
        assertThat(url.getPath()).isEqualTo("/" + PHOTO_KEY);
        assertThat(url.getQuery()).contains("X-Amz-Expires=300").contains("X-Amz-SignedHeaders=content-length;content-type;host")
            .doesNotContainIgnoringCase("checksum");
        assertThat(upload.headers()).containsOnly(entry("content-type", "image/jpeg"), entry("content-length", "1048576"));
        assertThat(upload.expiresAt()).isBetween(before.plusSeconds(299), Instant.now().plusSeconds(300));
    }
}
