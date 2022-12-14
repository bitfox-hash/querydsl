package com.querydsl;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.entity.Member;
import com.querydsl.entity.QMember;
import com.querydsl.entity.Team;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.entity.QMember.member;
import static com.querydsl.entity.QTeam.team;
import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Java6Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory jpaQueryFactory;

    @PersistenceUnit
    EntityManagerFactory emf;

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
                .where(member.username.eq("member1"))   //???????????? ????????? ??????
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    /**
     * ?????? ?????? ??????
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
     * ?????? ??????
     */
    @Test
    public void resultFetch() {
        //?????? ??????
        List<Member> fetchList = jpaQueryFactory
                .selectFrom(member)
                .fetch();

        //?????? ??????
        Member fetchOne = jpaQueryFactory.selectFrom(QMember.member)
                .fetchOne();

        //?????? ??? ??? ??????
        Member fetchFirst = jpaQueryFactory
                .selectFrom(QMember.member)
                .fetchFirst();

        //??????????????? ?????? - ?????????
        /*
        QueryResults<Member> results = jpaQueryFactory
                .selectFrom(member)
                .fetchResults();
        */

        //count ????????? ?????? - ?????????
        /*
        long count = jpaQueryFactory
                .selectFrom(member)
                .fetchCount();
        */
    }

    /**
     * ????????? ??????
     */
    @Test
    public void count() {
        Long totalCount = jpaQueryFactory
                .select(member.count())
                .from(member)
                .fetchOne();

        assertThat(totalCount).isEqualTo(4);
    }

    /**
     * ?????? ??????
     * 1. ?????? ?????? ???????????? desc
     * 2. ?????? ?????? ?????? ?????? asc
     * ??? 2?????? ?????? ????????? ????????? ???????????? ??????(nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * ?????????
     * ?????? ?????? ??????
     */
    @Test
    public void paging1() {
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    /**
     * JPQL
     * select
     * COUNT(m), //?????????
     * SUM(m.age), //?????? ???
     * AVG(m.age), //?????? ??????
     * MAX(m.age), //?????? ??????
     * MIN(m.age) //?????? ?????? * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = jpaQueryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                ).from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = jpaQueryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * ????????????
     *???A??? ????????? ?????? ??????
     */
    @Test
    public void join() {
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result).extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * ?????? ??????(??????????????? ?????? ????????? ??????) * ????????? ????????? ??? ????????? ?????? ?????? ??????
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = jpaQueryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ???) ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     *2. ???????????? ?????? ????????? ?????? ??????
     *???)????????? ????????? ?????? ????????? ?????? ?????? ?????? ??????
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = jpaQueryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * ???????????? ?????????
     */
    @Test
    public void fetchJoinNo() throws Exception {
        em.flush();
        em.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("???????????? ?????????").isFalse();
    }

    /**
     * ???????????? ??????
     */
    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();

        Member findMember = jpaQueryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("???????????? ??????").isTrue();
    }

    /**
     * ???????????? eq
     * ????????? ?????? ?????? ?????? ??????
     */
    @Test
    public void subQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(
                        member.age.eq(
                                select(memberSub.age.max())
                                        .from(memberSub))
                ).fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * ???????????? goe
     * ????????? ?????? ?????? ????????? ??????
     */
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(
                        member.age.goe(
                                select(memberSub.age.avg())
                                        .from(memberSub))
                ).fetch();

        assertThat(result).extracting("age").containsExactly(30, 40);
    }

    /**
     * ???????????? ?????? ??? ??????, in
     */
    @Test
    public void subQueryIn() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = jpaQueryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age").containsExactly(20, 30, 40);
    }

    /**
     * select ?????? subquery
     */
    @Test
    public void selectSubQuery() throws Exception {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = jpaQueryFactory
                .select(
                        member.username, select(memberSub.age.avg()).from(memberSub)
                ).from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println("age = " + tuple.get(select(memberSub.age.avg()).from(memberSub)));
        }
    }

    @Test
    public void basicCase() {
        List<String> fetch = jpaQueryFactory
                .select(
                        member.age.when(10).then("??????")
                                .when(20).then("?????????")
                                .otherwise("??????")
                ).from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase() {
        jpaQueryFactory.select(
                new CaseBuilder().when(member.age.between(0, 20)).then("0 ~ 20???")
                .when(member.age.between(21, 30)).then("21 ~ 30")
                .otherwise("??????"))
                .from(member)
                .fetch();
    }

    @Test
    public void constant() {
        Tuple a = jpaQueryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetchFirst();

        System.out.print(a);
    }

    @Test
    public void concat() {
        String s = jpaQueryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        System.out.println("s========>" + s);
    }
}
