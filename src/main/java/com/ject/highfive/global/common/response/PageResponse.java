package com.ject.highfive.global.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

// offset 페이징 응답. page는 1부터 센다 (Spring Data의 0부터 시작하는 번호에 +1)
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages, boolean hasNext) {

  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber() + 1,
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.hasNext());
  }
}
