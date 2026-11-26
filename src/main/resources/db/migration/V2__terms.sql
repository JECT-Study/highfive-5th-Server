-- 회원 약관과 약관 동의 기록 (JECT-213, 결정 #34·#35·#37)
-- 약관을 개정하면 같은 code로 새 버전 행을 추가한다 (이전 버전은 지우지 않음)

create table terms (
    id           bigint       not null auto_increment,
    -- SERVICE / PRIVACY / AGE_OVER_14 … (약관 목록은 데이터라 ENUM이 아닌 문자열)
    code         varchar(30)  not null,
    version      varchar(20)  not null,
    title        varchar(100) not null,
    is_required  bit          not null,
    -- 약관 전문(공개 웹페이지) 주소. PM이 URL을 주기 전까지는 NULL
    content_url  varchar(500),
    -- 이 시각이 지난 버전 중 가장 최근 것이 현재 버전
    effective_at datetime(6)  not null,
    primary key (id),
    constraint uk_terms_code_version unique (code, version)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

create table user_term_agreements (
    id        bigint      not null auto_increment,
    user_id   bigint      not null,
    term_id   bigint      not null,
    -- 선택 약관은 거부(0)도 기록한다
    agreed    bit         not null,
    agreed_at datetime(6) not null,
    primary key (id),
    constraint uk_user_term_agreements_user_term unique (user_id, term_id),
    constraint fk_user_term_agreements_user foreign key (user_id) references users (id),
    constraint fk_user_term_agreements_term foreign key (term_id) references terms (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci;

-- 회원 가입 필수 약관 3개 (결정 #34). 마케팅(선택)은 결정 #82(PM 확인) 후 추가한다.
-- 약관 문서의 시행일은 2026-12-01이지만, dev에서 바로 온보딩을 쓰도록 첫 버전은 2026-11-01부터 시행으로 둔다
insert into terms (code, version, title, is_required, content_url, effective_at) values
    ('SERVICE',     '1.0', '서비스 이용약관',          1, null, '2026-11-01 00:00:00'),
    ('PRIVACY',     '1.0', '개인정보 수집·이용 동의',   1, null, '2026-11-01 00:00:00'),
    ('AGE_OVER_14', '1.0', '만 14세 이상입니다',        1, null, '2026-11-01 00:00:00');
