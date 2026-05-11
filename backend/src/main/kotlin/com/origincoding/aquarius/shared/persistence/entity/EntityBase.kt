package com.origincoding.aquarius.shared.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import jakarta.persistence.Version
import org.hibernate.annotations.SoftDelete
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Suppress("unused")
@SoftDelete
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class EntityBase(
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(length = 36)
    var uuid: String? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @CreatedBy
    @Column(length = 64, nullable = false, updatable = false)
    var createdBy: String = SYSTEM_AUDITOR,

    @LastModifiedBy
    @Column(length = 64, nullable = false)
    var updatedBy: String = SYSTEM_AUDITOR,
) {
    @get:Transient
    val id: String
        get() = uuid ?: ""

    companion object {
        const val SYSTEM_AUDITOR = "SYSTEM"
    }
}
