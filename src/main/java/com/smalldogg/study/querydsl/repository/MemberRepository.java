package com.smalldogg.study.querydsl.repository;

import com.smalldogg.study.querydsl.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    //select m from Member m where m.username=?
    List<Member> findByUsername(String username);
}
