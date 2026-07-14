# JPA 주말 학습 프로젝트 가이드

## 프로젝트 목표

1~2일 동안 JPA의 핵심 개념을 학습할 수 있는 **미니 쇼핑몰 주문 관리 API**를 구현한다.

단순 게시판보다 연관관계, 영속성 전이, 지연 로딩, 변경 감지, 트랜잭션을 골고루 경험할 수 있다. 결제·배송·인증은 주말 범위를 벗어나므로 상품 주문과 재고 관리까지만 구현한다.

Spring Data JPA의 Repository 추상화, 쿼리 메서드, JPQL과 Hibernate의 엔티티 매핑, 연관관계, 조회 전략, 영속성 컨텍스트를 중심으로 학습한다.

## 권장 기술 구성

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Validation
- H2 Database
- Lombok
- JUnit 5

## 핵심 도메인

### Member

- `id`
- `name`
- `email`

### Product

- `id`
- `name`
- `price`
- `stockQuantity`

### Order

- `id`
- `member`
- `orderDate`
- `status`
- `orderItems`

### OrderItem

- `id`
- `order`
- `product`
- `orderPrice`
- `quantity`

### 연관관계

```text
Member  1 ─── N Order
Order   1 ─── N OrderItem
Product 1 ─── N OrderItem
```

`OrderItem`을 별도 엔티티로 둔다. 주문과 상품의 다대다 관계를 `@ManyToMany`로 직접 구현하지 않고, 주문 가격과 수량을 가진 연결 엔티티로 풀어낸다.

## 필수 기능

### 1. 회원 등록 및 조회

```http
POST /members
GET /members/{id}
```

학습 요소:

- `@Entity`
- 기본 키 생성 전략
- 유니크 이메일 검증
- `JpaRepository`

### 2. 상품 등록, 재고 수정 및 조회

```http
POST  /products
PATCH /products/{id}/stock
GET   /products
```

학습 요소:

- 엔티티 생성
- 변경 감지
- 트랜잭션
- 페이징과 정렬

재고 수정 시 별도의 `save()` 호출보다 트랜잭션 안에서 조회한 엔티티의 값을 변경하여, 변경 감지로 `UPDATE`가 실행되는지 확인한다.

### 3. 주문 생성

```http
POST /orders
```

요청 예시:

```json
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
```

주문 처리 순서:

```text
회원 조회
→ 상품 조회
→ 재고 확인
→ 상품 재고 감소
→ OrderItem 생성
→ Order 생성
→ 주문 저장
```

학습 요소:

- 연관관계 편의 메서드
- 영속성 전이 `CascadeType.PERSIST`
- 고아 객체 제거
- 변경 감지
- 트랜잭션 원자성
- 도메인 로직의 위치

재고 감소는 서비스에서 필드를 직접 수정하지 말고 엔티티의 도메인 로직으로 구현한다.

```java
public void decreaseStock(int quantity) {
    if (stockQuantity < quantity) {
        throw new IllegalStateException("재고가 부족합니다.");
    }

    stockQuantity -= quantity;
}
```

### 4. 주문 조회

```http
GET /orders/{id}
GET /orders?memberName=yun&status=ORDERED
```

학습 요소:

- 지연 로딩
- N+1 문제
- Fetch Join
- DTO 조회
- 쿼리 메서드 또는 `@Query`

메서드 이름 기반 쿼리, 선언형 쿼리, 정렬, DTO 프로젝션 등 여러 조회 방식을 실습한다.

### 5. 주문 취소

```http
POST /orders/{id}/cancel
```

처리 순서:

```text
주문 상태를 CANCELED로 변경
→ 각 주문 상품의 재고 복구
```

학습 요소:

- 변경 감지
- 연관된 엔티티 탐색
- 트랜잭션
- 상태 기반 도메인 로직

## 권장 일정

### 1일 차 오전: 프로젝트와 엔티티 구성

진행 순서:

1. 프로젝트 생성
2. H2 연결
3. 엔티티 네 개 작성
4. 연관관계 설정
5. 테이블 생성 SQL 확인
6. Repository 작성

REST API보다 먼저 엔티티와 테스트를 통해 데이터가 저장되는지 확인한다.

### 1일 차 오후: 주문 비즈니스 로직

구현 대상:

- 회원 등록
- 상품 등록
- 주문 생성
- 재고 감소
- 주문 취소
- 재고 복구

반드시 작성할 테스트:

- 상품 주문 성공
- 재고 부족 시 주문 실패
- 주문 취소 시 재고 복구
- 주문 저장 시 `OrderItem` 함께 저장

### 2일 차 오전: 조회와 성능 문제

먼저 일반 조회를 구현한다.

```java
Order order = orderRepository.findById(orderId)
        .orElseThrow();
```

API 응답을 만들면서 다음을 확인한다.

- 지연 로딩된 연관관계는 언제 조회되는가?
- SQL이 몇 번 실행되는가?
- 주문 상품을 반복 조회하면 N+1이 발생하는가?
- Fetch Join을 사용하면 SQL이 어떻게 달라지는가?

Fetch Join 예시:

```java
@Query("""
        select distinct o
        from Order o
        join fetch o.member
        join fetch o.orderItems oi
        join fetch oi.product
        where o.id = :orderId
        """)
Optional<Order> findDetailById(Long orderId);
```

### 2일 차 오후: API와 예외 처리

구현 대상:

- 요청 DTO
- 응답 DTO
- Bean Validation
- 전역 예외 처리
- 페이징
- README
- Postman 또는 HTTP 테스트 파일

엔티티를 API 응답으로 직접 반환하지 말고 DTO로 변환한다. 이 과정에서 지연 로딩과 JSON 직렬화 문제를 확인한다.

## 완료 기준

- [ ] 엔티티 연관관계를 직접 설계했다.
- [ ] `@ManyToOne`과 `@OneToMany`를 사용했다.
- [ ] 주문 생성이 하나의 트랜잭션으로 처리된다.
- [ ] 변경 감지로 재고가 수정된다.
- [ ] Cascade의 적용 여부를 설명할 수 있다.
- [ ] 주문 상세 조회에서 N+1을 재현했다.
- [ ] Fetch Join으로 N+1을 개선했다.
- [ ] 엔티티와 API DTO를 분리했다.
- [ ] 핵심 서비스 테스트를 작성했다.

## 제외 범위

- 인증
- 결제
- Docker
- Redis
- QueryDSL
- 프론트엔드

기능 수를 늘리는 것보다 하나의 주문 처리 흐름에서 JPA가 언제 `INSERT`, `SELECT`, `UPDATE`를 실행하는지 직접 확인하는 데 집중한다.

## 다른 프로젝트 후보

### 난이도 하: 도서 대여 시스템

도메인:

- `Member`
- `Book`
- `Loan`

기능:

- 도서 등록
- 대여
- 반납
- 연체 도서 조회

구현이 빠르지만 연관관계가 비교적 단순하다.

### 난이도 중: 일정 관리 시스템

도메인:

- `User`
- `Project`
- `Task`
- `Tag`
- `TaskTag`

기능:

- 프로젝트 생성
- 할 일 등록
- 담당자 지정
- 태그 지정
- 상태별 조회

동적 검색 조건과 다대다 연결 엔티티를 연습하기 좋다.

### 난이도 중상: 좌석 예약 시스템

도메인:

- `User`
- `Performance`
- `Seat`
- `Reservation`

기능:

- 공연 등록
- 좌석 조회
- 좌석 예약
- 예약 취소

낙관적 락 또는 비관적 락을 실험할 수 있지만, 동시성 처리까지 구현하면 2일을 넘기기 쉽다.

## 테스트 구현 가이드

테스트는 다음 순서로 진행한다.

```text
엔티티 매핑 테스트
→ Repository 테스트
→ 주문 서비스 통합 테스트
→ 동시성 테스트
```

### 1. 엔티티 저장 테스트

먼저 테이블과 엔티티 매핑이 정상인지 확인한다.

```java
@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 회원을_저장한다() {
        Member member = new Member("yun", "yun@example.com");

        Member savedMember = memberRepository.save(member);

        assertThat(savedMember.getId()).isNotNull();
        assertThat(savedMember.getName()).isEqualTo("yun");
        assertThat(savedMember.getEmail()).isEqualTo("yun@example.com");
        assertThat(savedMember.getCreatedAt()).isNotNull();
        assertThat(savedMember.getUpdatedAt()).isNotNull();
    }
}
```

Auditing을 사용한다면 테스트에서도 활성화한다.

```java
@TestConfiguration
@EnableJpaAuditing
class JpaAuditingTestConfig {
}

@DataJpaTest
@Import(JpaAuditingTestConfig.class)
class MemberRepositoryTest {
}
```

애플리케이션 설정 클래스에서 이미 `@EnableJpaAuditing`이 로딩된다면 별도 설정이 필요하지 않을 수 있다.

### 2. 상품 재고 단위 테스트

도메인 로직은 DB 없이 단위 테스트부터 작성한다. 이 테스트에는 `@SpringBootTest`나 `@DataJpaTest`가 필요 없다.

```java
class ProductTest {

    @Test
    void 재고를_감소한다() {
        Product product = new Product("키보드", 50_000L, 10L);

        product.decreaseStock(3L);

        assertThat(product.getStockQuantity()).isEqualTo(7L);
    }

    @Test
    void 재고보다_많이_주문하면_실패한다() {
        Product product = new Product("키보드", 50_000L, 2L);

        assertThatThrownBy(() -> product.decreaseStock(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("재고가 부족합니다.");
    }

    @Test
    void 재고를_복구한다() {
        Product product = new Product("키보드", 50_000L, 10L);

        product.increaseStock(3L);

        assertThat(product.getStockQuantity()).isEqualTo(13L);
    }
}
```

### 3. 주문 저장과 Cascade 테스트

`Order`에서 `OrderItem`으로 `CascadeType.PERSIST`를 설정했다면 주문만 저장해도 주문 항목이 저장되는지 확인한다.

```java
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void 주문과_주문항목을_함께_저장한다() {
        Member member = new Member("yun", "yun@example.com");
        Product product = new Product("키보드", 50_000L, 10L);

        entityManager.persist(member);
        entityManager.persist(product);

        OrderItem orderItem =
                new OrderItem(product, product.getPrice(), 2L);

        Order order = new Order(member);
        order.addOrderItem(orderItem);

        orderRepository.save(order);

        entityManager.flush();
        entityManager.clear();

        Order foundOrder = orderRepository.findById(order.getId())
                .orElseThrow();

        assertThat(foundOrder.getOrderItems()).hasSize(1);

        OrderItem foundItem = foundOrder.getOrderItems().getFirst();

        assertThat(foundItem.getProduct().getId())
                .isEqualTo(product.getId());
        assertThat(foundItem.getQuantity()).isEqualTo(2L);
        assertThat(foundItem.getOrderPrice()).isEqualTo(50_000L);
    }
}
```

`flush()`와 `clear()`를 사용하는 이유:

- `flush()`: 실제 `INSERT` SQL을 실행한다.
- `clear()`: 영속성 컨텍스트를 초기화한다.
- 다시 조회: 1차 캐시가 아닌 DB에 실제로 저장됐는지 검증한다.

### 4. 연관관계 테스트

양방향 관계를 사용한다면 양쪽 객체의 상태가 모두 맞는지 테스트한다.

```java
class OrderTest {

    @Test
    void 주문항목을_추가하면_양쪽_연관관계가_설정된다() {
        Product product = new Product("마우스", 30_000L, 10L);
        OrderItem orderItem = new OrderItem(product, 30_000L, 2L);

        Order order = new Order();
        order.addOrderItem(orderItem);

        assertThat(order.getOrderItems()).contains(orderItem);
        assertThat(orderItem.getOrder()).isSameAs(order);
    }
}
```

연관관계 편의 메서드 예시:

```java
public void addOrderItem(OrderItem orderItem) {
    orderItems.add(orderItem);
    orderItem.assignOrder(this);
}
```

### 5. 주문 서비스 통합 테스트

주문 생성은 여러 엔티티와 트랜잭션이 연결되므로 `@SpringBootTest`로 확인한다.

```java
@SpringBootTest
@Transactional
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void 상품을_주문한다() {
        Member member = memberRepository.save(
                new Member("yun", "yun@example.com")
        );

        Product product = productRepository.save(
                new Product("키보드", 50_000L, 10L)
        );

        Long orderId = orderService.order(
                member.getId(),
                List.of(new OrderItemRequest(product.getId(), 2L))
        );

        Order order = orderRepository.findById(orderId)
                .orElseThrow();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(product.getStockQuantity()).isEqualTo(8L);
    }
}
```

### 6. 주문 취소 테스트

```java
@Test
void 주문을_취소하면_상태가_바뀌고_재고가_복구된다() {
    Member member = memberRepository.save(
            new Member("yun", "yun@example.com")
    );

    Product product = productRepository.save(
            new Product("키보드", 50_000L, 10L)
    );

    Long orderId = orderService.order(
            member.getId(),
            List.of(new OrderItemRequest(product.getId(), 3L))
    );

    assertThat(product.getStockQuantity()).isEqualTo(7L);

    orderService.cancel(orderId);

    Order order = orderRepository.findById(orderId)
            .orElseThrow();

    assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
    assertThat(product.getStockQuantity()).isEqualTo(10L);
}
```

이미 취소된 주문을 다시 취소할 수 없게 했다면 다음도 확인한다.

```java
@Test
void 이미_취소된_주문은_다시_취소할_수_없다() {
    Long orderId = createOrder();

    orderService.cancel(orderId);

    assertThatThrownBy(() -> orderService.cancel(orderId))
            .isInstanceOf(IllegalStateException.class);
}
```

### 7. 트랜잭션 롤백 테스트

재고 차감 중 일부 상품에서 실패하면 주문 전체가 저장되지 않아야 한다. 예를 들어 상품 두 개를 주문할 때 두 번째 상품의 재고가 부족한 경우를 검증한다.

```java
@Test
void 주문_중_재고가_부족하면_전체가_롤백된다() {
    Member member = memberRepository.save(
            new Member("yun", "yun@example.com")
    );

    Product product1 = productRepository.save(
            new Product("키보드", 50_000L, 10L)
    );

    Product product2 = productRepository.save(
            new Product("마우스", 30_000L, 1L)
    );

    assertThatThrownBy(() ->
            orderService.order(
                    member.getId(),
                    List.of(
                            new OrderItemRequest(product1.getId(), 2L),
                            new OrderItemRequest(product2.getId(), 5L)
                    )
            )
    ).isInstanceOf(IllegalStateException.class);

    assertThat(product1.getStockQuantity()).isEqualTo(10L);
    assertThat(product2.getStockQuantity()).isEqualTo(1L);
    assertThat(orderRepository.count()).isZero();
}
```

이 테스트가 통과하려면 주문 서비스 메서드에 트랜잭션이 있어야 한다.

```java
@Transactional
public Long order(...) {
}
```

### 8. N+1 테스트

주문 여러 개를 저장한 뒤 목록 조회 시 SQL 개수를 직접 확인한다.

```java
@Test
void 주문_목록을_조회한다() {
    List<Order> orders = orderRepository.findAll();

    for (Order order : orders) {
        order.getOrderItems().size();

        for (OrderItem item : order.getOrderItems()) {
            item.getProduct().getName();
        }
    }
}
```

로그가 다음과 같은 형태라면 N+1이 발생한 것이다.

```sql
select * from orders;

select * from order_items where order_id = ?;
select * from order_items where order_id = ?;
select * from order_items where order_id = ?;
```

`@BatchSize` 적용 후에는 다음과 같이 바뀌는지 확인한다.

```sql
select *
from order_items
where order_id in (?, ?, ?);
```

Hibernate Statistics로 쿼리 수를 검증할 수도 있지만, 학습 초기에는 SQL 로그를 직접 확인한다.

## 최소 테스트 목록

주말 프로젝트에서는 다음 여섯 개를 우선 작성한다.

1. 회원 저장 성공
2. 상품 재고 감소 성공
3. 재고 부족 시 예외
4. 주문과 `OrderItem` Cascade 저장
5. 주문 취소 시 상태 변경과 재고 복구
6. 주문 실패 시 전체 트랜잭션 롤백

테스트 의존성은 일반적으로 Spring Boot 프로젝트에 기본 포함된다.

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

AssertJ는 다음 정적 import를 사용한다.

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```

## 최종 권장 범위

**미니 주문 관리 API + H2 + 서비스 테스트 + Fetch Join 1회**까지 구현한다.
