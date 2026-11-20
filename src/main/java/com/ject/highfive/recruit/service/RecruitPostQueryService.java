package com.ject.highfive.recruit.service;

import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.concert.repository.JoinedTeamCount;
import com.ject.highfive.global.common.response.PageResponse;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.repository.RecruitPostFavoriteRepository;
import com.ject.highfive.recruit.repository.RecruitPostRepository;
import com.ject.highfive.recruit.repository.RecruitPostSpecifications;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 공고 목록(2.5). 쿼리는 페이지마다 최대 5번으로 고정된다:
// 공고+공연, 개수, 장르(@BatchSize), 합류 팀 수, 찜 여부 (스펙 4.5)
// open-in-view가 꺼져 있어 장르 지연 로딩이 끝나도록 응답을 트랜잭션 안에서 만든다
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitPostQueryService {

  // 같은 초에 만든 공고의 순서를 id로 고정한다
  private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private final RecruitPostRepository recruitPostRepository;
  private final ConcertTeamRepository concertTeamRepository;
  private final RecruitPostFavoriteRepository favoriteRepository;

  public PageResponse<RecruitPostSummaryResponse> search(
      RecruitPostSearchRequest request, int page, int size, Long userId) {
    Page<RecruitPost> posts =
        recruitPostRepository.findAll(
            RecruitPostSpecifications.search(
                request.region(), request.genres(), request.careerLevels(), request.keyword()),
            PageRequest.of(page - 1, size, LATEST));

    Map<Long, Integer> joinedTeamCounts = joinedTeamCounts(posts.getContent());
    Set<Long> favoritedPostIds = favoritedPostIds(userId, posts.getContent());
    LocalDate today = LocalDate.now();
    return PageResponse.from(
        posts.map(
            post ->
                RecruitPostSummaryResponse.from(
                    post,
                    joinedTeamCounts.getOrDefault(post.getConcert().getId(), 0),
                    favoritedPostIds.contains(post.getId()),
                    today)));
  }

  private Map<Long, Integer> joinedTeamCounts(List<RecruitPost> posts) {
    if (posts.isEmpty()) {
      return Map.of();
    }
    List<Long> concertIds = posts.stream().map(post -> post.getConcert().getId()).toList();
    return concertTeamRepository
        .countGuestTeamsByConcertIds(concertIds, ConcertTeamStatus.JOINED)
        .stream()
        .collect(
            Collectors.toMap(
                JoinedTeamCount::getConcertId, count -> count.getJoinedTeamCount().intValue()));
  }

  private Set<Long> favoritedPostIds(Long userId, List<RecruitPost> posts) {
    if (posts.isEmpty()) {
      return Set.of();
    }
    return favoriteRepository.findRecruitPostIdsByUserId(
        userId, posts.stream().map(RecruitPost::getId).toList());
  }
}
