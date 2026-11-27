package com.ject.highfive.recruit.service;

import com.ject.highfive.concert.dummy.DummyBand;
import com.ject.highfive.concert.dummy.DummyBandStore;
import com.ject.highfive.concert.entity.ConcertTeam;
import com.ject.highfive.concert.entity.ConcertTeamStatus;
import com.ject.highfive.concert.repository.ConcertTeamRepository;
import com.ject.highfive.concert.repository.JoinedTeamCount;
import com.ject.highfive.global.common.response.PageResponse;
import com.ject.highfive.global.exception.BusinessException;
import com.ject.highfive.recruit.dto.BandSearchRequest;
import com.ject.highfive.recruit.dto.BandSummaryResponse;
import com.ject.highfive.recruit.dto.RecruitPostDetailResponse;
import com.ject.highfive.recruit.dto.RecruitPostSearchRequest;
import com.ject.highfive.recruit.dto.RecruitPostSummaryResponse;
import com.ject.highfive.recruit.entity.RecruitPost;
import com.ject.highfive.recruit.exception.RecruitErrorCode;
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

// 공고 목록(2.5), 상세(2.7), 팀 검색(2.6).
// 목록은 페이지마다 쿼리 최대 5번: 공고+공연, 개수, 장르(@BatchSize), 합류 팀 수, 찜 여부 (221 스펙 4.5)
// 상세는 쿼리 5번: 공고+공연, 팀 유형, 공연 장르, 참여 팀, 찜 여부 (223 스펙 4.5)
// 팀 검색은 밴드 엔티티 전까지 DB 없이 DummyBandStore에서 거른다 (225 스펙 4.1, JECT-253에서 교체)
// open-in-view가 꺼져 있어 지연 로딩이 끝나도록 응답을 트랜잭션 안에서 만든다
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecruitPostQueryService {

  // 같은 초에 만든 공고의 순서를 id로 고정한다
  private static final Sort LATEST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

  private final RecruitPostRepository recruitPostRepository;
  private final ConcertTeamRepository concertTeamRepository;
  private final RecruitPostFavoriteRepository favoriteRepository;
  private final DummyBandStore dummyBandStore;

  public RecruitPostDetailResponse getDetail(Long recruitPostId, Long userId) {
    RecruitPost post =
        recruitPostRepository
            .findWithConcertByIdAndDeletedAtIsNullAndConcertDeletedAtIsNull(recruitPostId)
            .orElseThrow(() -> new BusinessException(RecruitErrorCode.RECRUIT_POST_NOT_FOUND));
    List<ConcertTeam> teams =
        concertTeamRepository.findLineup(post.getConcert().getId(), ConcertTeamStatus.JOINED);
    Map<Long, DummyBand> bands =
        dummyBandStore.findAllByIds(teams.stream().map(ConcertTeam::getBandId).toList());
    boolean favorite =
        favoriteRepository
            .findRecruitPostIdsByUserId(userId, List.of(recruitPostId))
            .contains(recruitPostId);
    return RecruitPostDetailResponse.from(post, teams, bands, favorite, LocalDate.now());
  }

  public PageResponse<RecruitPostSummaryResponse> search(
      RecruitPostSearchRequest request, int page, int size, Long userId) {
    // 마감 숨기기 조건과 응답 status를 같은 날짜로 계산한다
    LocalDate today = LocalDate.now();
    Page<RecruitPost> posts =
        recruitPostRepository.findAll(
            RecruitPostSpecifications.search(
                request.region(),
                request.genres(),
                request.careerLevels(),
                request.keyword(),
                request.hideClosed(),
                today),
            PageRequest.of(page - 1, size, LATEST));

    Map<Long, Integer> joinedTeamCounts = joinedTeamCounts(posts.getContent());
    Set<Long> favoritedPostIds = favoritedPostIds(userId, posts.getContent());
    return PageResponse.from(
        posts.map(
            post ->
                RecruitPostSummaryResponse.from(
                    post,
                    joinedTeamCounts.getOrDefault(post.getConcert().getId(), 0),
                    favoritedPostIds.contains(post.getId()),
                    today)));
  }

  public PageResponse<BandSummaryResponse> searchBands(
      BandSearchRequest request, int page, int size) {
    return PageResponse.from(
        dummyBandStore
            .search(
                request.keyword(),
                request.region(),
                request.genres(),
                request.careerLevels(),
                PageRequest.of(page - 1, size))
            .map(BandSummaryResponse::from));
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
