-- 기존 user_social_accounts 테이블이 있는 MySQL 8.4 DB에만 수동 적용한다.
-- 먼저 아래 조회를 별도로 실행해 실제 열 정의와 복합 UNIQUE 키를 확인한다.
-- SHOW CREATE TABLE user_social_accounts;
-- SHOW FULL COLUMNS FROM user_social_accounts LIKE 'provider_user_id';
--
-- 이미 utf8mb4_bin이면 적용할 필요가 없다. 열이 VARCHAR(255) NOT NULL이 아니거나
-- DEFAULT/COMMENT 등 별도 속성이 있으면 이 ALTER를 그대로 실행하지 않는다.
-- 쓰기를 중지하고 백업한 뒤, 변경 후 UNIQUE 기준으로 충돌할 기존 행이 없는지 확인한다.
-- SELECT COUNT(*) AS conflicting_groups
-- FROM (
--     SELECT 1
--     FROM user_social_accounts
--     GROUP BY provider, CONVERT(provider_user_id USING utf8mb4) COLLATE utf8mb4_bin
--     HAVING COUNT(*) > 1
-- ) AS collisions;
-- 결과가 0일 때만 아래 ALTER를 실행한다. 완료 후 열 collation과 복합 UNIQUE 키를 재확인한다.
ALTER TABLE user_social_accounts
    MODIFY COLUMN provider_user_id VARCHAR(255)
        CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL;
