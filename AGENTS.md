주말(1~2일)동안 진행할 JPA 학습 용도의 프로젝트 추천좀
가장 추천: 미니 쇼핑몰 주문 관리 API

단순 게시판보다 JPA의 핵심인 연관관계, 영속성 전이, 지연 로딩, 변경 감지, 트랜잭션을 골고루 경험할 수 있습니다. 다만 결제·배송·인증까지 넣으면 주말 범위를 벗어나므로, 상품 주문과 재고 관리까지만 구현하는 것이 좋습니다.

Spring Data JPA는 엔티티를 중심으로 Repository 추상화를 제공하며, 쿼리 메서드와 직접 정의한 JPQL 쿼리를 지원합니다. Hibernate 공식 문서에서도 엔티티 매핑, 연관관계, 조회 전략 및 영속성 컨텍스트를 주요 주제로 다룹니다.

핵심 도메인
Member
- id
- name
- email

Product
- id
- name
- price
- stockQuantity

Order
- id
- member
- orderDate
- status
- orderItems

OrderItem
- id
- order
- product
- orderPrice
- quantity
  연관관계
  Member 1 ─── N Order
  Order  1 ─── N OrderItem
  Product 1 ── N OrderItem

OrderItem을 별도 엔티티로 두는 것이 중요합니다. 주문과 상품의 다대다 관계를 직접 @ManyToMany로 구현하지 않고, 주문 가격과 수량을 가진 연결 엔티티로 풀어내는 연습을 할 수 있기 때문입니다.

필수 기능
1. 회원 등록
   POST /members
   GET /members/{id}

학습 요소:

@Entity
기본 키 생성 전략
유니크 이메일 검증
JpaRepository
2. 상품 등록 및 재고 수정
   POST /products
   PATCH /products/{id}/stock
   GET /products

학습 요소:

엔티티 생성
변경 감지
트랜잭션
페이징과 정렬

재고 수정은 별도의 save() 호출보다, 트랜잭션 안에서 조회한 엔티티의 값을 변경하여 변경 감지로 UPDATE가 실행되는지 확인해 보는 것이 좋습니다.

3. 주문 생성
   POST /orders

요청 예시:

{
"memberId": 1,
"items": [
{
"productId": 1,
"quantity": 2
},
{
"productId": 3,
"quantity": 1
}
]
}

주문 처리 순서:

회원 조회
→ 상품 조회
→ 재고 확인
→ 상품 재고 감소
→ OrderItem 생성
→ Order 생성
→ 주문 저장

학습 요소:

연관관계 편의 메서드
영속성 전이 CascadeType.PERSIST
고아 객체 제거
변경 감지
트랜잭션 원자성
도메인 로직의 위치

예를 들어 재고 감소는 서비스가 직접 필드를 수정하기보다 다음처럼 엔티티에 두는 것이 좋습니다.

public void decreaseStock(int quantity) {
if (stockQuantity < quantity) {
throw new IllegalStateException("재고가 부족합니다.");
}

    stockQuantity -= quantity;
}
4. 주문 조회
   GET /orders/{id}
   GET /orders?memberName=yun&status=ORDERED

학습 요소:

지연 로딩
N+1 문제
Fetch Join
DTO 조회
쿼리 메서드 또는 @Query

Spring Data JPA는 메서드 이름 기반 쿼리, 선언형 쿼리, 정렬 및 DTO 프로젝션 등 여러 조회 방식을 지원합니다.

5. 주문 취소
   POST /orders/{id}/cancel

처리 내용:

주문 상태 CANCELED 변경
→ 각 주문 상품의 재고 복구

학습 요소:

변경 감지
연관된 엔티티 탐색
트랜잭션
상태 기반 도메인 로직
1일 차 일정
오전: 프로젝트와 엔티티 구성

기술 구성:

Java 21
Spring Boot
Spring Web
Spring Data JPA
Validation
H2 Database
Lombok
JUnit 5

진행 순서:

프로젝트 생성
H2 연결
엔티티 네 개 작성
연관관계 설정
테이블 생성 SQL 확인
Repository 작성

이 단계에서는 REST API보다 먼저 엔티티와 테스트를 통해 데이터가 저장되는지 확인하세요. Spring의 공식 시작 가이드도 엔티티와 Repository를 이용한 데이터 저장·조회부터 구성합니다.

오후: 주문 비즈니스 로직

구현 대상:

회원 등록
상품 등록
주문 생성
재고 감소
주문 취소
재고 복구

반드시 작성할 테스트:

상품 주문 성공
재고 부족 시 주문 실패
주문 취소 시 재고 복구
주문 저장 시 OrderItem 함께 저장
2일 차 일정
오전: 조회와 성능 문제

먼저 다음처럼 일반 조회를 구현합니다.

Order order = orderRepository.findById(orderId)
.orElseThrow();

이후 API 응답을 만들면서 다음을 확인합니다.

지연 로딩된 연관관계는 언제 조회되는가?
SQL이 몇 번 실행되는가?
주문 상품을 반복 조회하면 N+1이 발생하는가?
Fetch Join을 사용하면 SQL이 어떻게 달라지는가?

예시:

@Query("""
select distinct o
from Order o
join fetch o.member
join fetch o.orderItems oi
join fetch oi.product
where o.id = :orderId
""")
Optional<Order> findDetailById(Long orderId);
오후: API와 예외 처리

구현 대상:

요청 DTO
응답 DTO
Bean Validation
전역 예외 처리
페이징
README
Postman 또는 HTTP 테스트 파일

엔티티를 API 응답으로 직접 반환하지 말고 DTO로 변환하세요. 이 과정에서 지연 로딩과 JSON 직렬화 문제도 자연스럽게 경험하게 됩니다.

완료 기준

주말 동안 다음 항목까지만 달성하면 충분합니다.

엔티티 연관관계를 직접 설계했다.
@ManyToOne과 @OneToMany를 사용했다.
주문 생성이 하나의 트랜잭션으로 처리된다.
변경 감지로 재고가 수정된다.
Cascade의 적용 여부를 설명할 수 있다.
주문 상세 조회에서 N+1을 재현했다.
Fetch Join으로 N+1을 개선했다.
엔티티와 API DTO를 분리했다.
핵심 서비스 테스트를 작성했다.
다른 프로젝트 후보
난이도 하: 도서 대여 시스템
Member
Book
Loan

기능:

도서 등록
대여
반납
연체 도서 조회

장점은 구현이 빠르다는 것이고, 단점은 연관관계가 비교적 단순하다는 것입니다.

난이도 중: 일정 관리 시스템
User
Project
Task
Tag
TaskTag

기능:

프로젝트 생성
할 일 등록
담당자 지정
태그 지정
상태별 조회

동적 검색 조건과 다대다 연결 엔티티를 연습하기 좋습니다.

난이도 중상: 좌석 예약 시스템
User
Performance
Seat
Reservation

기능:

공연 등록
좌석 조회
좌석 예약
예약 취소

낙관적 락 또는 비관적 락까지 실험할 수 있지만, 동시성 처리까지 하면 2일을 넘기기 쉽습니다.

최종 추천 범위

미니 주문 관리 API + H2 + 서비스 테스트 + Fetch Join 한 번이 가장 적절합니다.

인증, 결제, Docker, Redis, QueryDSL, 프론트엔드는 제외하세요. 주말 프로젝트의 목적은 기능 수를 늘리는 것이 아니라, 하나의 주문 처리 흐름을 통해 JPA가 언제 INSERT·SELECT·UPDATE를 실행하는지 직접 확인하는 것입니다.