package com.origincoding.aquarius.shared.persistence.entity

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Transient
import org.springframework.data.domain.AfterDomainEventPublication
import org.springframework.data.domain.DomainEvents

@Suppress("unused")
@MappedSuperclass
abstract class AggregateRootBase : EntityBase() {
    @field:Transient
    private val domainEvents: MutableList<Any> = mutableListOf()

    protected fun registerDomainEvent(event: Any) {
        domainEvents.add(event)
    }

    @DomainEvents
    protected fun domainEvents(): List<Any> = domainEvents.toList()

    @AfterDomainEventPublication
    protected fun afterDomainEventPublication() {
        domainEvents.clear()
    }
}
