package com.origincoding.aquarius.platform.redis

import org.redisson.codec.TypedJsonJackson3Codec
import tools.jackson.databind.ObjectMapper

/**
 * Creates a Redisson JSON codec that deserializes values as the requested concrete type.
 */
inline fun <reified T : Any> ObjectMapper.typedCodec(): TypedJsonJackson3Codec =
    TypedJsonJackson3Codec(T::class.java, this)
