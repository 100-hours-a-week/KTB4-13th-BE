package com.book.core.book.infrastructure.client;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

interface NationalLibraryClient {
    @GetExchange("/seoji/SearchApi.do?cert_key={nationalLibraryKey}&result_style=json&ebook_yn=N&form=종이책&sort=INPUT_DATE&order_by=ASC")
    NationalLibraryResponse searchPaperBooks(
        @RequestParam("start_publish_date") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate from,
        @RequestParam("end_publish_date") @DateTimeFormat(pattern = "yyyyMMdd") LocalDate through,
        @RequestParam("page_no") int pageNumber,
        @RequestParam("page_size") int pageSize
    );
}
