package com.book.core.book.application.port;

import com.book.core.book.application.result.NewBookImportReport;

public interface NewBookImportNotifier {
    void send(NewBookImportReport report);
}
