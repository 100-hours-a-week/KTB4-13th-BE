package com.book.core.search.infrastructure.client.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown=true)record AiSearchFallback(String message){}
