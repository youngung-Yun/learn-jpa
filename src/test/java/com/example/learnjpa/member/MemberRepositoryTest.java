package com.example.learnjpa.member;

import com.example.learnjpa.config.JpaConfig;
import com.example.learnjpa.order.Order;
import com.example.learnjpa.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@Import(JpaConfig.class)
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager entityManager;
    @Autowired
    private OrderRepository orderRepository;

    @Test
    @DisplayName("Member 매핑 테스트")
    void save_test() {

        var name = "name";
        var email = "test@example.com";

        Member member = Member.builder()
                .name(name)
                .email(email)
                .build();

        var saved = memberRepository.save(member);
        entityManager.flush();
        entityManager.clear();

        var found = memberRepository.findById(saved.getId())
                .orElseThrow();

        assertThat(found.getName()).isEqualTo(saved.getName());
        assertThat(found.getEmail()).isEqualTo(saved.getEmail());
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Member와 연관된 Order들 매핑 테스트")
    void orders_related_to_member() {
        var member = Member.builder()
                .name("name")
                .email("test@example.com")
                .build();

        member = memberRepository.save(member);

        Order order1 = new Order(member);
        Order order2 = new Order(member);

        order1 = orderRepository.save(order1);
        order2 = orderRepository.save(order2);

        entityManager.flush();
        entityManager.clear();

        var found = memberRepository.findById(member.getId())
                .orElseThrow();

        var orderList = found.getOrderList();

        assertThat(orderList).hasSize(2);
        assertThat(orderList)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(order1.getId(), order2.getId());
    }
}