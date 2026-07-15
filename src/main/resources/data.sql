-- 개발용 JPA 조회 학습 데이터
--
-- Spring Boot는 개발 프로필의 인메모리 H2를 시작할 때 이 파일을 V1__init.sql 다음에 실행한다.
-- 여러 주문에서 같은 회원/상품을 재사용해 1차 캐시의 영향도 볼 수 있고,
-- 주문마다 컬렉션을 두어 LAZY 목록 조회의 N+1을 재현할 수 있게 구성했다.
--
-- Flyway 도입 시 주의:
-- 1. schema.sql은 db/migration/V1__init.sql 같은 versioned migration으로 옮긴다.
-- 2. 이 파일은 학습용 샘플 데이터이므로 운영 공통 V2 마이그레이션으로 그대로 옮기지 않는다.
-- 3. 계속 필요하면 dev 프로필 전용 Flyway location 또는 테스트 fixture로 분리한다.
-- 4. 국가 코드처럼 모든 환경에 반드시 필요한 기준 데이터만 공통 migration으로 관리한다.

INSERT INTO members (name, email)
VALUES ('yun', 'yun@learn-jpa.test'),
       ('kim', 'kim@learn-jpa.test'),
       ('lee', 'lee@learn-jpa.test'),
       ('park', 'park@learn-jpa.test');

INSERT INTO products (name, price, stock_quantity)
VALUES ('기계식 키보드', 50000, 30),
       ('무선 마우스', 30000, 40),
       ('27인치 모니터', 300000, 12),
       ('노트북 거치대', 40000, 25),
       ('USB 허브', 60000, 18),
       ('웹캠', 120000, 10),
       ('헤드셋', 90000, 16),
       ('데스크 매트', 20000, 50);

-- ordered_at을 학습 데이터의 식별 기준으로 사용하므로 각 값은 서로 다르게 둔다.
INSERT INTO orders (member_id, ordered_at, status)
SELECT id, TIMESTAMP '2026-07-01 10:00:00', 'ORDERED'
FROM members
WHERE email = 'yun@learn-jpa.test';

INSERT INTO orders (member_id, ordered_at, status)
SELECT id, TIMESTAMP '2026-07-02 11:00:00', 'ORDERED'
FROM members
WHERE email = 'kim@learn-jpa.test';

INSERT INTO orders (member_id, ordered_at, status)
SELECT id, TIMESTAMP '2026-07-03 12:00:00', 'CANCELED'
FROM members
WHERE email = 'yun@learn-jpa.test';

INSERT INTO orders (member_id, ordered_at, status)
SELECT id, TIMESTAMP '2026-07-04 13:00:00', 'DELIVERED'
FROM members
WHERE email = 'lee@learn-jpa.test';

INSERT INTO orders (member_id, ordered_at, status)
SELECT id, TIMESTAMP '2026-07-05 14:00:00', 'ORDERED'
FROM members
WHERE email = 'park@learn-jpa.test';

INSERT INTO orders (member_id, ordered_at, status)
SELECT id, TIMESTAMP '2026-07-06 15:00:00', 'ORDERED'
FROM members
WHERE email = 'kim@learn-jpa.test';

INSERT INTO order_products (order_id, product_id, order_price, quantity)
VALUES
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-01 10:00:00'),
     (SELECT id FROM products WHERE name = '기계식 키보드'), 50000, 2),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-01 10:00:00'),
     (SELECT id FROM products WHERE name = '무선 마우스'), 30000, 1),

    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-02 11:00:00'),
     (SELECT id FROM products WHERE name = '27인치 모니터'), 300000, 1),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-02 11:00:00'),
     (SELECT id FROM products WHERE name = '노트북 거치대'), 40000, 1),

    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-03 12:00:00'),
     (SELECT id FROM products WHERE name = '기계식 키보드'), 48000, 1),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-03 12:00:00'),
     (SELECT id FROM products WHERE name = 'USB 허브'), 60000, 2),

    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-04 13:00:00'),
     (SELECT id FROM products WHERE name = '웹캠'), 120000, 1),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-04 13:00:00'),
     (SELECT id FROM products WHERE name = '헤드셋'), 90000, 1),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-04 13:00:00'),
     (SELECT id FROM products WHERE name = '데스크 매트'), 20000, 2),

    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-05 14:00:00'),
     (SELECT id FROM products WHERE name = '무선 마우스'), 28000, 2),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-05 14:00:00'),
     (SELECT id FROM products WHERE name = 'USB 허브'), 58000, 1),

    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-06 15:00:00'),
     (SELECT id FROM products WHERE name = '27인치 모니터'), 290000, 1),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-06 15:00:00'),
     (SELECT id FROM products WHERE name = '기계식 키보드'), 50000, 1),
    ((SELECT id FROM orders WHERE ordered_at = TIMESTAMP '2026-07-06 15:00:00'),
     (SELECT id FROM products WHERE name = '데스크 매트'), 18000, 1);
