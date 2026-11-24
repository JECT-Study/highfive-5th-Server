-- 첫 스키마 (JECT-257). 엔티티 기준 Hibernate(MySQL) DDL을 옮기고 제약 이름만 읽기 쉽게 바꿨다
-- 이미 적용된 마이그레이션 파일은 고치지 않는다. 엔티티를 바꾸면 V2__설명.sql처럼 새 파일을 추가한다
-- 문자열 enum 컬럼은 MySQL ENUM이라 enum 상수를 추가·변경하면 ALTER TABLE ... MODIFY COLUMN도 필요하다

-- 회원 ---------------------------------------------------------------------------

create table users (
    id                   bigint       not null auto_increment,
    -- 영문 대소문자를 구분한다 (결정 #32). UNIQUE도 같은 기준
    nickname             varchar(10) collate utf8mb4_bin,
    profile_image_url    varchar(500),
    status               enum ('ACTIVE','PENDING','WITHDRAWN') not null,
    push_enabled         bit          not null,
    push_chat_enabled    bit          not null,
    push_invite_enabled  bit          not null,
    push_concert_enabled bit          not null,
    push_ticket_enabled  bit          not null,
    withdrawn_at         datetime(6),
    created_at           datetime(6)  not null,
    updated_at           datetime(6)  not null,
    primary key (id),
    constraint uk_users_nickname unique (nickname)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table user_social_accounts (
    id               bigint       not null auto_increment,
    user_id          bigint       not null,
    provider         enum ('APPLE','GOOGLE','KAKAO') not null,
    -- 소셜 ID 토큰의 sub. 대소문자를 구분한다
    provider_user_id varchar(255) collate utf8mb4_bin not null,
    email            varchar(255),
    created_at       datetime(6)  not null,
    updated_at       datetime(6)  not null,
    primary key (id),
    constraint uk_user_social_accounts_provider_user unique (provider, provider_user_id),
    constraint fk_user_social_accounts_user foreign key (user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table user_genres (
    id          bigint      not null auto_increment,
    user_id     bigint      not null,
    genre_code  enum ('ETC','INDIE','JAZZ','METAL','POP','PUNK','ROCK') not null,
    custom_name varchar(10),
    primary key (id),
    constraint uk_user_genres_user_genre unique (user_id, genre_code),
    constraint fk_user_genres_user foreign key (user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table user_positions (
    id            bigint      not null auto_increment,
    user_id       bigint      not null,
    position_code enum ('BASS','CUSTOM','DRUM','GUITAR','KEYBOARD','VOCAL') not null,
    custom_name   varchar(10),
    primary key (id),
    constraint uk_user_positions_user_position unique (user_id, position_code),
    constraint fk_user_positions_user foreign key (user_id) references users (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- 공연 ---------------------------------------------------------------------------

create table concerts (
    id                 bigint       not null auto_increment,
    host_band_id       bigint       not null,
    created_by         bigint       not null,
    updated_by         bigint       not null,
    version            bigint       not null,
    title              varchar(100) not null,
    recruit_team_count integer      not null,
    perform_date       date,
    start_time         time(0),
    end_time           time(0),
    region             varchar(50),
    place              varchar(100),
    address            varchar(200),
    rental_fee         integer,
    poster_image_url   varchar(500),
    apply_url          varchar(500),
    status             enum ('ENDED','PREPARING','SELLING') not null,
    deleted_at         datetime(6),
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    primary key (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table concert_genres (
    concert_id bigint not null,
    genre_code enum ('ETC','INDIE','JAZZ','METAL','POP','PUNK','ROCK') not null,
    primary key (concert_id, genre_code),
    constraint fk_concert_genres_concert foreign key (concert_id) references concerts (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table concert_teams (
    id           bigint      not null auto_increment,
    concert_id   bigint      not null,
    band_id      bigint      not null,
    lineup_order integer,
    status       enum ('JOINED','KICKED','LEFT') not null,
    created_at   datetime(6) not null,
    updated_at   datetime(6) not null,
    primary key (id),
    constraint uk_concert_teams_concert_band unique (concert_id, band_id),
    constraint fk_concert_teams_concert foreign key (concert_id) references concerts (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- 모집 공고 ----------------------------------------------------------------------

create table recruit_posts (
    id                  bigint       not null auto_increment,
    concert_id          bigint       not null,
    -- 살아 있는 공고만 값이 있어 공연당 살아 있는 공고를 하나로 막는다 (의사결정 D13)
    active_concert_id   bigint,
    author_user_id      bigint       not null,
    wanted_career_level enum ('ANY','OVER_3Y','UNDER_1Y','Y1_TO_3') not null,
    description         varchar(500) not null,
    status              enum ('CLOSED','RECRUITING') not null,
    deadline            date         not null,
    deleted_at          datetime(6),
    created_at          datetime(6)  not null,
    updated_at          datetime(6)  not null,
    primary key (id),
    constraint uk_recruit_posts_active_concert unique (active_concert_id),
    constraint fk_recruit_posts_concert foreign key (concert_id) references concerts (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table recruit_post_team_types (
    post_id   bigint not null,
    team_type enum ('ACTIVE','ANYONE','EXPERIENCED','FIRST_PERFORMANCE','HOBBY','ORIGINAL_SONGS') not null,
    primary key (post_id, team_type),
    constraint fk_recruit_post_team_types_post foreign key (post_id) references recruit_posts (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table recruitment_favorites (
    id                  bigint      not null auto_increment,
    user_id             bigint      not null,
    recruitment_post_id bigint      not null,
    created_at          datetime(6) not null,
    updated_at          datetime(6) not null,
    primary key (id),
    constraint uk_recruitment_favorites_user_post unique (user_id, recruitment_post_id),
    constraint fk_recruitment_favorites_post foreign key (recruitment_post_id) references recruit_posts (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;
