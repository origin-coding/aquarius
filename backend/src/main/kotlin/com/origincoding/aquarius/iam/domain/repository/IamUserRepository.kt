package com.origincoding.aquarius.iam.domain.repository

import com.origincoding.aquarius.iam.domain.model.IamUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.stereotype.Repository

@Repository
interface IamUserRepository : JpaRepository<IamUser, String>, QuerydslPredicateExecutor<IamUser>
