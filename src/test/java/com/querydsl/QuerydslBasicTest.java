package com.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.entity.Member;
import com.querydsl.entity.QMember;
import com.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static com.querydsl.entity.QMember.member;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory jpaQueryFactory;

    @BeforeEach
    public void before() {
        jpaQueryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        Member findMember = jpaQueryFactory.select(member)
                .from(member)
                .where(member.username.eq("member1"))   //파라미터 바인딩 처리
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * 기본 검색 쿼리
     */
    @Test
    public void search() {
        Member findMember = jpaQueryFactory.selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        Member findMember = jpaQueryFactory.selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        (member.age.eq(10))
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * 결과 조회
     */
    @Test
    public void resultFetch() {
        //목록 조회
        List<Member> fetchList = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        //단건 조회
        Member fetchOne = jpaQueryFactory.selectFrom(QMember.member)
                .fetchOne();

        //처음 한 건 조회
        Member fetchFirst = jpaQueryFactory
                .selectFrom(QMember.member)
                .fetchFirst();

        //페이징에서 사용 - 미사용
        QueryResults<Member> results = jpaQueryFactory
                .selectFrom(member)
                .fetchResults();

        //count 쿼리로 변경 - 미사용
        long count = jpaQueryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * 카운트 조회
     */
    @Test
    public void count() {
        Long totalCount = jpaQueryFactory
                .select(member.count())
                .from(member)
                .fetchOne();
    }
}
