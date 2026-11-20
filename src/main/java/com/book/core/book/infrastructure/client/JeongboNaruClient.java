package com.book.core.book.infrastructure.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;

interface JeongboNaruClient {
    @GetExchange("/api/srchDtlList?authKey={jeongboNaruKey}&loaninfoYN=N&format=json")
    JeongboNaruResponse findBookDetail(@RequestParam("isbn13") String isbn13);
}
