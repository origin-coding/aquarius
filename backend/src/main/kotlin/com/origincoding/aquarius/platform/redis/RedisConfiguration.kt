package com.origincoding.aquarius.platform.redis

import org.redisson.codec.JsonJackson3Codec
import org.redisson.spring.starter.RedissonAutoConfigurationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper

@Configuration(proxyBeanMethods = false)
class RedisConfiguration(
    private val objectMapper: ObjectMapper,
) {
    @Bean
    fun redissonAutoConfigurationCustomizer(): RedissonAutoConfigurationCustomizer =
        RedissonAutoConfigurationCustomizer { config ->
            // JsonJackson3Codec writes JSON with type metadata for polymorphic deserialization.
            // Prefer typed codecs in business code when the stored value type is known.
            config.codec = JsonJackson3Codec(objectMapper)
        }
}
