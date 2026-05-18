package com.origincoding.aquarius.iam.infrastructure.session

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "aquarius.iam.session")
class IamSessionProperties(
    val accessTokenTtl: Duration = Duration.ofMinutes(30),
    val refreshTokenTtl: Duration = Duration.ofHours(2)
)
