package com.ject.highfive.concert.dummy;

import com.ject.highfive.concert.entity.Genre;
import java.util.List;

// 필드 이름은 ERD bands·band_genres와 맞춘다. memberCount = declared_member_count (JECT-253에서 실제 Band로 교체)
public record DummyBand(
    Long id, String name, String profileImageUrl, List<Genre> genres, int memberCount) {}
