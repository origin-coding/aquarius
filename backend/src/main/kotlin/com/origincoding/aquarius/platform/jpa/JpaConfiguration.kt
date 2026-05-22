package com.origincoding.aquarius.platform.jpa

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.SharedEntityManagerCreator

@Configuration
class JpaConfiguration {
    @Bean
    fun entityManager(entityManagerFactory: EntityManagerFactory): EntityManager =
        SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory)
}
