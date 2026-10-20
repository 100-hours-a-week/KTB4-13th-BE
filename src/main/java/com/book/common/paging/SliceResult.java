package com.book.common.paging;

import java.util.List;

/** 저장소 기술 타입을 노출하지 않는 페이지 내용과 다음 페이지 여부. */
public record SliceResult<T>(List<T> content, boolean hasNext) {
}
