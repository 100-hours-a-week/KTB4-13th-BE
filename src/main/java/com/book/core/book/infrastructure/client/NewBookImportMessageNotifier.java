package com.book.core.book.infrastructure.client;

import com.book.common.notification.NotificationSender;
import com.book.core.book.application.port.NewBookImportNotifier;
import com.book.core.book.application.result.NewBookImportReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class NewBookImportMessageNotifier implements NewBookImportNotifier {
    private final NotificationSender sender;

    @Override
    public void send(final NewBookImportReport report) {
        final String summary = """
            신간 도서 등록 배치가 %s.
            runId=%s
            소요 시간: %d ms
            후보 ISBN: %d건 | 상세 조회: %d건 | 신규 등록: %d건
            처리 결과: %s
            """.formatted(report.status(), report.runId(), report.elapsedMs(), report.candidateCount(), report.detailCallCount(),
            report.registeredCount(), report.outcomes());
        String content = summary;
        if (report.failed()) {
            content += "실패 사유: " + report.failureReason();
        }
        sender.send(content);
    }
}
