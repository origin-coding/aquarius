package com.origincoding.aquarius.iam.domain.repository

import com.origincoding.aquarius.iam.domain.model.Identity
import com.origincoding.aquarius.iam.domain.model.IdentityType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.stereotype.Repository

@Repository
interface IdentityRepository : JpaRepository<Identity, String>, QuerydslPredicateExecutor<Identity> {
    fun findByIdentityTypeAndNormalizedIdentity(identityType: IdentityType, normalizedIdentity: String): Identity?

    fun existsByIdentityTypeAndNormalizedIdentity(identityType: IdentityType, normalizedIdentity: String): Boolean
}
