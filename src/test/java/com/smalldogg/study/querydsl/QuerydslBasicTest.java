package com.smalldogg.study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.smalldogg.study.querydsl.dto.MemberDto;
import com.smalldogg.study.querydsl.dto.UserDto;
import com.smalldogg.study.querydsl.dto.QMemberDto;
import com.smalldogg.study.querydsl.entity.Member;
import com.smalldogg.study.querydsl.entity.QMember;
import com.smalldogg.study.querydsl.entity.QTeam;
import com.smalldogg.study.querydsl.entity.Team;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
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
    void startJPQL() {
        //member1??? ?????????.
        String qlString = "select m from Member m " +
                "where m.username=:username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQueryDsl() {
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))//???????????? ????????? ??????
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void search() {
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1")
                        .and(QMember.member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(
                        QMember.member.username.eq("member1"),
                        QMember.member.age.eq(10))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(QMember.member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
//                .limit(1).fetch()
                .fetchFirst();

        QueryResults<Member> result = queryFactory
                .selectFrom(QMember.member)
                .fetchResults();
        result.getTotal();

        queryFactory
                .selectFrom(QMember.member)
                .fetchCount();

        Long totalCount = queryFactory
                .select(QMember.member.count())
                .from(QMember.member)
                .fetchOne();
    }

    /**
     * ?????? ?????? ??????
     * 1. ?????? ?????? ????????????(desc)
     * 2. ?????? ?????? ????????????(asc)
     * ??? 2?????? ?????? ????????? ????????? ???????????? ??????(nulls last)
     */
    @Test
    void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.age.eq(100))
                .orderBy(QMember.member.age.desc(), QMember.member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    void paging1() {
        List<Member> result = queryFactory
                .selectFrom(QMember.member)
                .orderBy(QMember.member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(QMember.member)
                .orderBy(QMember.member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    @Test
    void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        QMember.member.count(),
                        QMember.member.age.sum(),
                        QMember.member.age.avg(),
                        QMember.member.age.max(),
                        QMember.member.age.min()
                )
                .from(QMember.member)
                .fetch();

        Tuple tuple = result.get(0);
        Assertions.assertThat(tuple.get(QMember.member.count())).isEqualTo(4);
        Assertions.assertThat(tuple.get(QMember.member.age.sum())).isEqualTo(100);
        Assertions.assertThat(tuple.get(QMember.member.age.avg())).isEqualTo(25);
        Assertions.assertThat(tuple.get(QMember.member.age.max())).isEqualTo(40);
        Assertions.assertThat(tuple.get(QMember.member.age.min())).isEqualTo(10);

    }

    /**
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????.
     */
    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(QTeam.team.name, QMember.member.age.avg())
                .from(QMember.member)
                .join(QMember.member.team, QTeam.team)
                .groupBy(QTeam.team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        Assertions.assertThat(teamA.get(QTeam.team.name)).isEqualTo("teamA");
        Assertions.assertThat(teamA.get(QMember.member.age.avg())).isEqualTo(15);
        Assertions.assertThat(teamB.get(QTeam.team.name)).isEqualTo("teamB");
        Assertions.assertThat(teamB.get(QMember.member.age.avg())).isEqualTo(35);
    }

    /**
     * ??? A??? ????????? ?????? ????????? ?????????
     */
    @Test
    void join() {
        List<Member> result = queryFactory
                .selectFrom(QMember.member)
                .join(QMember.member.team, QTeam.team)
                .where(QTeam.team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * ?????? ??????(theta join)
     * ??????????????? ?????? ?????? ???????????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ??????
     * * ?????? ?????? ????????? -> on?????? ???????????? ?????? ?????? ??????
     */
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(QMember.member)
                .from(QMember.member, QTeam.team)
                .where(QMember.member.username.eq(QTeam.team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * JPQL : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(QMember.member, QTeam.team)
                .from(QMember.member)
                .leftJoin(QMember.member.team, QTeam.team)
                .on(QTeam.team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * ???????????? ?????? ????????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
     */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(QMember.member, QTeam.team)
                .from(QMember.member)
                .leftJoin(QTeam.team).on(QMember.member.username.eq(QTeam.team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("fetch join ?????????").isFalse();

    }

    @Test
    void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .join(QMember.member.team, QTeam.team).fetchJoin() //???????????? ??????
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("fetch join ??????").isTrue();

    }

    /**
     * ????????? ?????? ?????? ?????? ??????
     */
    @Test
    void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result.get(0).getAge()).isEqualTo(40);
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * ????????? ?????? ????????? ?????? ??????
     */
    @Test
    void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * ????????? 10 chrhk ?????? ??????
     */
    @Test
    void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(QMember.member.username,
                        select(memberSub.age.avg()).from(memberSub))
                .from(QMember.member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    //Simple Case
    @Test
    void basicCase() {

        List<String> result = queryFactory
                .select(QMember.member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????")
                )
                .from(QMember.member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(QMember.member.age.between(10, 20)).then("0~20???")
                        .when(QMember.member.age.between(21, 30)).then("21~30???")
                        .otherwise("??????")
                ).from(QMember.member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(QMember.member.username, Expressions.constant("A"))
                .from(QMember.member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    void concat() {
        //{username}_{age}
        List<String> result = queryFactory
                .select(QMember.member.username.concat("_").concat(QMember.member.age.stringValue()))
                .from(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void simpleProjection() {
        List<String> result = queryFactory
                .select(QMember.member.username)
                .from(QMember.member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tupleProjection() {
        List<Tuple> result = queryFactory
                .select(QMember.member.username, QMember.member.age)
                .from(QMember.member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(QMember.member.username);
            Integer age = tuple.get(QMember.member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }

    }

    @Test
    void findDtoByJPQL() {
        //JPQL??? new operation??? ???????????? ??????.
        List<MemberDto> result = em.createQuery("select new com.smalldogg.study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoBySetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, QMember.member.username, QMember.member.age))
                .from(QMember.member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, QMember.member.username, QMember.member.age))
                .from(QMember.member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, QMember.member.username, QMember.member.age))
                .from(QMember.member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    void findUserDtoByField() {
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        QMember.member.username.as("name"),
                        //???????????? ????????? ?????????????????? ?????? ??????
                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(QMember.member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findUserDtoByConstructor() {
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class, QMember.member.username, QMember.member.age))
                .from(QMember.member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(QMember.member.username, QMember.member.age))
                .from(QMember.member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * BooleanBuilder ??????
     */
    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(QMember.member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(QMember.member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(QMember.member)
                .where(builder)
                .fetch();
    }

    //???????????? BooleanBuilder ?????? ??? ?????? ?????????
    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(QMember.member)
//                .where(usernameEq(usernameCond), ageEq(ageCond))
                .where(allEq(usernameCond,ageCond))
                .fetch();


    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? QMember.member.username.eq(usernameCond) : null;
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? QMember.member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    //???????????? : ?????? ???????????? ?????? ????????? ??????

    @Test
    void bulkUpdate() {

        //????????? ?????? ????????? ???
        long count = queryFactory
                .update(QMember.member)
                .set(QMember.member.username, "?????????")
                .where(QMember.member.age.lt(28))
                .execute();

        //?????? ????????? ????????? ??????????????? ???????????? DB??? ???????????? ?????????, ????????? ??????????????? ?????????????????????.
        //repeatable read??? ???????????? ?????? ???????????? ?????????.
        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(QMember.member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    void bulkAdd() {
        queryFactory
                .update(QMember.member)
                .set(QMember.member.age, QMember.member.age.add(1))
//                .set(member.age, member.age.multiply(1))
                .execute();
    }

    @Test
    void bulkDelete() {
        long count = queryFactory
                .delete(QMember.member)
                .where(QMember.member.age.gt(18))
                .execute();
    }

    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(
                        Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                                QMember.member.username, "member", "M"))
                .from(QMember.member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunction2() {
        List<String> result = queryFactory
                .select(QMember.member.username)
                .from(QMember.member)
                .where(QMember.member.username.eq(QMember.member.username.lower())
//                .where(member.username.eq(Expressions.stringTemplate("function('lower',{0})", member.username))
                )
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
