package com.origincoding.aquarius.iam.domain.repository

import com.origincoding.aquarius.iam.domain.model.Credential
import com.origincoding.aquarius.iam.domain.model.CredentialType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.stereotype.Repository

@Repository
interface CredentialRepository : JpaRepository<Credential, String>, QuerydslPredicateExecutor<Credential> {
    fun findByUserIdAndCredentialType(userId: String, credentialType: CredentialType): Credential?
}
