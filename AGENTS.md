# JPA 조회 최적화와 Bean Validation 학습 가이드

## 현재 학습 상태

이 프로젝트는 미니 쇼핑몰 주문 도메인을 사용한다.

다음 내용은 이미 구현하고 학습했으므로 이번 커리큘럼에서 반복하지 않는다.

- `Member`, `Product`, `Order`, `OrderProduct` 엔티티와 연관관계
- Spring Data JPA Repository와 기본 서비스 로직
- 주문 생성·취소, 재고 감소·복구
- 영속성 컨텍스트, 변경 감지, Cascade
- 트랜잭션과 롤백
- 엔티티, Repository, 서비스 통합 테스트

앞으로는 기존 코드를 확장해 다음 두 주제를 집중적으로 학습한다.

1. JPA 조회 시 발생하는 N+1 문제와 조회 전략 비교
2. Jakarta Bean Validation을 이용한 요청 데이터 검증

마지막에는 JPA의 스키마 자동 생성과 다른 역할을 하는 Flyway를 짧게 실습한다.

## 프로젝트 기준

- Java 25
- Spring Boot 4.1
- Spring Data JPA와 Hibernate
- Jakarta Bean Validation
- H2, PostgreSQL 드라이버
- JUnit 5와 AssertJ

현재 도메인 관계는 다음과 같다.

```text
Member 1 ── N Order
Order 1 ── N OrderProduct
Product 1 ── N OrderProduct
```

### 실행 시 준비되는 학습 데이터

개발 프로필에서는 `schema.sql`로 테이블을 만든 뒤 `data.sql`로 다음 데이터를 자동 입력한다.

- 회원 4명
- 상품 8개
- 주문 6개
- 주문 상품 14개
- 같은 회원과 상품을 일부 주문에서 반복 사용한 데이터

인메모리 H2이므로 애플리케이션을 재시작할 때마다 데이터베이스가 초기화되고 같은 데이터가 다시 입력된다. Repository와 서비스 테스트에서는 `src/test/resources/application.yaml`의 `spring.sql.init.mode=never`로 이 데이터를 넣지 않아 테스트 격리를 유지한다.

N+1을 관찰할 때는 주문 목록을 조회한 뒤 `member`, `orderProductList`, `OrderProduct.product` 순서로 접근한다. 같은 회원과 상품을 다시 조회할 때 1차 캐시 때문에 추가 SQL이 생략되는 경우도 함께 확인한다.

이번 학습에서는 인증, 결제, 프론트엔드, Docker, Redis, QueryDSL 같은 기능을 추가하지 않는다. JPA 조회 전략이나 Bean Validation을 실습하는 데 꼭 필요한 최소한의 웹 계층만 작성한다.

## 최종 학습 목표

- 일반 LAZY 조회에서 N+1이 발생하는 시점과 이유를 SQL로 설명한다.
- Fetch Join, `@EntityGraph`, `@BatchSize`의 동작과 장단점을 비교한다.
- 단건 상세 조회와 페이징 목록 조회에 적절한 전략을 선택한다.
- API 요청 DTO에 표준 제약 조건을 선언하고 중첩 객체까지 검증한다.
- Bean Validation, 데이터베이스 제약 조건, 도메인 규칙의 책임 차이를 설명한다.
- Flyway가 데이터베이스 스키마 변경 이력을 관리하는 도구임을 이해한다.

## 1단계: 일반 조회로 N+1 재현

### 학습 목적

`FetchType.LAZY`는 연관 엔티티를 즉시 가져오지 않을 뿐, 연관 데이터가 필요할 때 실행되는 추가 쿼리까지 없애지는 않는다. 주문 목록을 한 번 조회한 뒤 각 주문의 회원, 주문 상품, 상품을 순회하며 어떤 시점에 SQL이 실행되는지 확인한다.

### 실습 순서

1. 회원과 상품을 여러 개 만든다.
2. 서로 다른 회원의 주문을 3개 이상 저장한다.
3. `flush()`와 `clear()`로 1차 캐시의 영향을 제거한다.
4. 일반 `findAll()` 또는 별도의 일반 JPQL로 주문 목록을 조회한다.
5. 아래 순서로 연관관계에 접근하며 SQL 수를 기록한다.

```java
List<Order> orders = orderRepository.findAll();

for (Order order : orders) {
    order.getMember().getName();

    for (OrderProduct orderProduct : order.getOrderProductList()) {
        orderProduct.getProduct().getName();
    }
}
```

### 확인할 내용

- 최초 주문 목록 조회 쿼리는 몇 번인가?
- `member`에 접근할 때 회원 수만큼 추가 쿼리가 발생하는가?
- `orderProductList`를 초기화할 때 주문 수만큼 추가 쿼리가 발생하는가?
- 각 `product`를 초기화할 때 추가 쿼리가 발생하는가?
- 같은 엔티티가 1차 캐시에 이미 있으면 쿼리 수가 왜 달라지는가?
- 트랜잭션 밖에서 LAZY 연관관계에 접근하면 어떤 문제가 생기는가?

SQL 로그를 먼저 직접 읽는다. 비교 테스트를 안정적으로 자동화하고 싶다면 선택적으로 Hibernate Statistics의 `prepareStatementCount`를 사용한다. 테스트마다 통계를 초기화하고, 쿼리 수뿐 아니라 조회 결과도 함께 검증한다.

## 2단계: Fetch Join 적용

현재 `OrderRepository.findDetailById()`의 Fetch Join을 기준으로 일반 조회와 비교한다.

```java
@Query("""
        select distinct o
        from Order o
        join fetch o.member
        join fetch o.orderProductList op
        join fetch op.product
        where o.id = :id
        """)
Optional<Order> findDetailById(@Param("id") Long id);
```

### 실습

- 동일한 주문을 `findById()`와 `findDetailById()`로 각각 조회한다.
- 각 테스트 전에 영속성 컨텍스트를 비운다.
- 응답 DTO를 만들 때까지 발생한 SQL을 비교한다.
- `distinct`를 제거했을 때 루트 엔티티 결과가 어떻게 보이는지 확인한다.

### 반드시 이해할 내용

- Fetch Join은 JPQL에서 해당 쿼리의 로딩 전략을 명시한다.
- 단건 상세 조회에서 필요한 연관관계를 한 번에 가져오기에 적합하다.
- 컬렉션 Fetch Join은 결과 행을 증가시킨다.
- 컬렉션 Fetch Join과 페이징을 함께 사용하면 메모리 페이징이나 예외로 이어질 수 있으므로 목록 조회에 무조건 적용하지 않는다.
- 둘 이상의 컬렉션을 동시에 Fetch Join하면 Hibernate의 bag 관련 제한을 만날 수 있다.

## 3단계: `@EntityGraph` 적용

`@EntityGraph`는 JPQL에 `join fetch`를 직접 작성하지 않고 Repository 메서드에서 조회할 연관관계 그래프를 선언하는 방법이다.

```java
@EntityGraph(attributePaths = {
        "member",
        "orderProductList",
        "orderProductList.product"
})
@Query("select distinct o from Order o where o.id = :id")
Optional<Order> findDetailWithEntityGraphById(@Param("id") Long id);
```

단순 조회에서는 파생 쿼리에도 적용해 본다.

```java
@EntityGraph(attributePaths = "member")
List<Order> findAllByStatus(OrderStatus status);
```

### 실습

- Fetch Join 메서드와 같은 연관관계를 가져오는 EntityGraph 메서드를 만든다.
- 두 방식의 SQL과 조회 결과를 비교한다.
- 목록 조회에서는 먼저 `member` 같은 ToOne 연관관계만 그래프에 포함해 페이징과 함께 확인한다.
- 필요하면 `@NamedEntityGraph`도 한 번 작성해 재사용 가능한 그래프와 동적 `attributePaths`의 차이를 확인한다.

### 반드시 이해할 내용

- EntityGraph도 결국 필요한 연관관계를 한 쿼리에서 가져오도록 힌트를 주는 방식이다.
- Repository 메서드가 간결해지고 같은 조건 쿼리에 서로 다른 로딩 그래프를 적용하기 쉽다.
- 문자열 기반 `attributePaths`는 리팩터링 시 컴파일 타임 검사를 받지 못한다.
- 컬렉션을 그래프에 포함하면 Fetch Join과 마찬가지로 행 증가와 페이징 문제를 고려해야 한다.
- `FETCH` 그래프와 `LOAD` 그래프가 기본 매핑의 fetch 설정을 다루는 방식의 차이를 확인한다.

## 4단계: `@BatchSize` 적용

`@BatchSize`는 LAZY 로딩을 유지하면서 아직 초기화되지 않은 여러 프록시나 컬렉션을 `IN` 쿼리로 묶어 조회한다. N+1을 완전히 한 쿼리로 바꾸는 것이 아니라, 추가 쿼리 수를 배치 단위로 줄이는 전략이다.

컬렉션에 적용하는 예시:

```java
@BatchSize(size = 100)
@OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
private List<OrderProduct> orderProductList = new ArrayList<>();
```

ToOne 대상 엔티티를 배치 로딩하려면 대상 엔티티 클래스에 적용할 수 있다.

```java
@Entity
@BatchSize(size = 100)
public class Product {
    // ...
}
```

애너테이션 대신 Hibernate 전역 설정도 비교할 수 있다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

### 실습

- 일반 조회의 LAZY 접근 코드는 그대로 둔다.
- `Order.orderProductList`에 `@BatchSize`를 적용하기 전후 SQL을 비교한다.
- `OrderProduct.product` 접근에서 발생하는 ToOne N+1도 별도로 확인한다.
- 주문 수를 배치 크기보다 크게 만들어 `1 + ceil(N / batchSize)` 형태로 쿼리가 나뉘는지 관찰한다.
- 페이징된 주문 목록에서도 LAZY 로딩과 페이징이 유지되는지 확인한다.

배치 크기를 무조건 크게 잡지 않는다. 실제 한 페이지 크기, DB의 `IN` 절 제한, 한 번에 적재되는 엔티티 수를 고려한다.

## 조회 전략 비교 정리

| 전략 | 연관관계 로딩 | 주 사용처 | 장점 | 주의점 |
|---|---|---|---|---|
| 일반 LAZY 조회 | 접근 시 개별 조회 | N+1 재현, 연관 데이터가 불필요한 조회 | 필요한 시점까지 로딩 지연 | 반복 접근 시 N+1 가능 |
| Fetch Join | 지정한 연관관계를 같은 JPQL에서 조회 | 단건 상세, 조회 형태가 고정된 쿼리 | SQL 의도가 명확하고 한 번에 조회 가능 | 컬렉션 페이징, 행 증가, 다중 컬렉션 제한 |
| `@EntityGraph` | 선언한 그래프에 따라 함께 조회 | 파생 쿼리, Repository별 로딩 계획 | 쿼리 조건과 로딩 계획을 분리 | 문자열 경로, 컬렉션 페이징 문제 |
| `@BatchSize` | LAZY 접근 시 `IN` 쿼리로 묶음 | 페이징 목록 뒤 연관 컬렉션 사용 | LAZY와 페이징을 유지하며 쿼리 감소 | 한 쿼리가 되지는 않으며 배치 크기 조정 필요 |

권장 실습 결론을 미리 정하지 않는다. 같은 데이터와 같은 DTO 변환을 기준으로 SQL 수, 결과 행 수, 페이징 가능 여부를 측정한 뒤 용도별 선택 근거를 작성한다.

## 5단계: Jakarta Bean Validation

### 학습 목적

Bean Validation은 애플리케이션 경계로 들어온 값의 형식과 기본 조건을 선언적으로 검증한다. 엔티티의 비즈니스 메서드나 데이터베이스 제약 조건을 대체하지 않는다.

필요하다면 Validation 스타터를 추가한다.

```kotlin
implementation("org.springframework.boot:spring-boot-starter-validation")
```

### 요청 DTO 제약 조건

현재 요청 record에 다음 조건을 적용한다.

- `SignupRequest.name`: `@NotBlank`, `@Size(max = 50)`
- `SignupRequest.email`: `@NotBlank`, `@Email`, `@Size(max = 255)`
- `ProductRegisterRequest.name`: `@NotBlank`, `@Size(max = 100)`
- `ProductRegisterRequest.price`: `@NotNull`, `@PositiveOrZero`
- `ProductRegisterRequest.stockQuantity`: `@NotNull`, `@PositiveOrZero`
- `OrderCreateRequest.memberId`: `@NotNull`, `@Positive`
- `OrderCreateRequest.createOrderProductList`: `@NotEmpty`, `@Valid`
- 주문 항목의 `productId`: `@NotNull`, `@Positive`
- 주문 항목의 `quantity`: `@NotNull`, `@Positive`

중첩 DTO 검증 예시:

```java
public record OrderCreateRequest(
        @NotNull @Positive Long memberId,
        @NotEmpty List<@Valid OrderCreateProductRequest> createOrderProductList
) {
    public record OrderCreateProductRequest(
            @NotNull @Positive Long productId,
            @NotNull @Positive Long quantity
    ) {
    }
}
```

실제 사용하는 Validation 구현체에서 컨테이너 요소의 `@Valid` 지원 형태를 확인한다. 가장 읽기 쉬운 형태를 선택하고, 반드시 잘못된 중첩 주문 항목으로 테스트한다.

### 검증 실행 지점

최소 Controller를 만들어 요청 본문에 `@Valid`를 적용한다.

```java
@PostMapping("/orders")
public OrderDetailsResponse create(@Valid @RequestBody OrderCreateRequest request) {
    return orderService.create(request);
}
```

서비스 메서드의 파라미터·반환값 검증도 학습하려면 `@Validated`를 사용해 메서드 검증을 별도로 실습한다. 웹 요청 검증과 메서드 검증을 한 테스트에서 섞지 않는다.

### 제약 조건별 책임 구분

| 규칙 | 주 책임 위치 | 이유 |
|---|---|---|
| 이름이 공백이 아님, 이메일 형식 | 요청 DTO의 Bean Validation | 입력 형식 오류를 경계에서 빠르게 거절 |
| 수량이 양수임 | 요청 DTO와 필요 시 DB `CHECK` | 요청 오류를 설명하고 저장 데이터도 보호 |
| 이메일이 유일함 | DB `UNIQUE` + 서비스의 친절한 오류 변환 | 동시 요청에서 애너테이션 기반 사전 조회만으로 보장 불가 |
| 재고가 주문 수량보다 충분함 | `Product.decreaseStock()` 도메인 로직 | 현재 상태에 따라 달라지는 비즈니스 규칙 |
| 취소 가능한 주문 상태임 | `Order.cancel()` 도메인 로직 | 엔티티 상태 전이에 관한 규칙 |
| 컬럼의 `NOT NULL`, 길이, FK | 데이터베이스와 JPA 매핑 | 최종 데이터 무결성 보장 |

`@NotNull`과 `@Column(nullable = false)`는 목적과 실행 시점이 다르다. 둘 중 하나만 선택하는 관계가 아니라, 애플리케이션 경계와 저장소 경계에서 각각 책임을 가진다.

### Validation 테스트

1. `jakarta.validation.Validator`를 사용하는 빠른 단위 테스트를 작성한다.
2. 정상 DTO의 위반 개수가 0인지 검증한다.
3. 공백 이름, 잘못된 이메일, 음수 가격, 빈 주문 목록을 각각 검증한다.
4. 주문 목록은 유효하지만 내부 항목의 수량이 0일 때 중첩 검증이 실패하는지 확인한다.
5. 최소 웹 계층 테스트에서 `@Valid`가 실제 HTTP 요청에 적용되는지 확인한다.

Validation 오류 응답의 대규모 공통 포맷이나 복잡한 전역 예외 처리 체계는 이번 범위가 아니다. 어떤 필드가 어떤 제약 조건을 위반했는지 테스트에서 확인할 수 있는 최소 구현만 둔다.

### 선택 학습

표준 제약 조건을 충분히 익힌 뒤 하나만 선택한다.

- Validation Group으로 생성과 수정 규칙 분리
- 클래스 수준 사용자 정의 제약 조건으로 서로 연관된 두 필드 검증
- 서비스 메서드 파라미터와 반환값 검증

단순한 조건을 사용자 정의 애너테이션으로 만들지 않는다. `@NotBlank`, `@Size`, `@Positive` 같은 표준 제약 조건을 우선 사용한다.

## 6단계: Flyway 입문

### Flyway란 무엇인가

Flyway는 데이터베이스 스키마와 기준 데이터의 변경을 버전이 붙은 마이그레이션 파일로 관리하는 도구다. 실행된 마이그레이션은 스키마 히스토리 테이블에 버전, 성공 여부, 체크섬 등으로 기록된다.

Hibernate의 `ddl-auto=create` 또는 `update`가 엔티티 매핑을 바탕으로 현재 스키마를 만들거나 변경하는 기능이라면, Flyway는 개발자가 작성한 변경 이력을 정해진 순서로 각 환경에 재현한다. 운영에 가까운 환경에서는 Flyway가 변경을 적용하고 Hibernate는 `ddl-auto=validate`로 엔티티와 스키마의 일치 여부만 검사하는 구성을 학습한다.

### 최소 실습

1. `org.flywaydb:flyway-core` 의존성을 추가한다.
2. 기존 `schema.sql`을 `src/main/resources/db/migration/V1__init.sql`로 옮겨 최초 마이그레이션으로 사용한다.
3. 개발 학습용 `data.sql`은 운영 공통 V2로 옮기지 않는다. 필요하면 dev 프로필 전용 Flyway location 또는 테스트 fixture로 분리한다.
4. 모든 환경에 필요한 기준 데이터만 별도의 versioned migration으로 관리한다.
5. `spring.sql.init.mode=never`로 `schema.sql`/`data.sql` 자동 초기화와 Flyway가 중복 실행되지 않게 한다.
6. Hibernate의 `ddl-auto=validate`는 유지해 엔티티와 Flyway 스키마의 일치 여부를 검사한다.
7. `flyway_schema_history` 테이블과 V1 적용 기록을 확인한다.
8. `V2__add_product_description.sql`을 만들어 컬럼 하나를 추가하고 순차 적용을 확인한다.
9. 이미 적용된 V1 파일을 수정했을 때 체크섬 검증이 실패하는지 확인한 뒤 원상 복구한다.

이미 공유 환경에 적용된 versioned migration은 수정하지 않고 새로운 버전으로 변경을 추가하는 원칙을 익힌다. Flyway의 고급 배포, 자동 롤백, 유료 기능은 이번 범위에서 제외한다.

## 권장 진행 순서

### 1일 차: JPA 조회 최적화

1. 일반 LAZY 조회로 N+1 재현
2. 기존 Fetch Join과 SQL 비교
3. 같은 조회를 `@EntityGraph`로 구현
4. `@BatchSize`와 전역 batch fetch size 비교
5. 단건 상세와 페이징 목록에 대한 선택 기준 정리

### 2일 차 오전: Bean Validation

1. Validation 의존성과 실행 환경 확인
2. 회원·상품·주문 요청 DTO에 표준 제약 조건 적용
3. `@Valid`를 이용한 중첩 주문 항목 검증
4. Validator 단위 테스트와 최소 웹 계층 테스트 작성
5. DTO 검증, 도메인 규칙, DB 제약의 책임 비교

### 2일 차 오후: 정리와 Flyway 맛보기

1. 조회 전략별 SQL과 장단점 문서화
2. Flyway V1, V2 마이그레이션 실행
3. Hibernate `ddl-auto=validate`와 함께 동작 확인

## 최소 테스트 목록

기존 엔티티·서비스·Repository 테스트는 유지하고 다음 테스트만 추가한다.

1. 일반 목록 조회에서 N+1 재현
2. Fetch Join 상세 조회의 결과와 SQL 수 확인
3. EntityGraph 조회의 결과와 SQL 수 확인
4. `@BatchSize` 적용 전후 추가 쿼리 수 확인
5. 정상 요청 DTO Validation 성공
6. 회원·상품의 각 대표 제약 조건 실패
7. 빈 주문 항목 목록 검증 실패
8. 중첩 주문 항목의 0 수량 검증 실패
9. Flyway V1과 V2 순차 적용 및 Hibernate 스키마 검증

## 완료 기준

- [ ] 일반 LAZY 조회에서 N+1을 재현하고 SQL 발생 시점을 설명할 수 있다.
- [ ] Fetch Join으로 단건 주문 상세를 조회하고 일반 조회와 비교했다.
- [ ] 같은 요구사항을 `@EntityGraph`로 구현하고 Fetch Join과 비교했다.
- [ ] `@BatchSize`가 생성하는 `IN` 쿼리와 배치 단위 쿼리 수를 확인했다.
- [ ] 컬렉션 Fetch Join과 페이징을 함께 사용할 때의 문제를 설명할 수 있다.
- [ ] 상황별로 세 조회 전략 중 하나를 선택하는 근거를 작성했다.
- [ ] 요청 DTO에 표준 Bean Validation 제약 조건을 적용했다.
- [ ] `@Valid`로 중첩 주문 항목을 검증했다.
- [ ] Bean Validation, 도메인 규칙, DB 제약 조건의 책임을 구분할 수 있다.
- [ ] Flyway 마이그레이션 두 개를 적용하고 스키마 히스토리를 확인했다.

## 최종 권장 범위

**N+1 재현 + Fetch Join/EntityGraph/BatchSize 비교 + 요청 DTO Bean Validation과 테스트 + Flyway V1/V2 입문**까지 완료한다.
