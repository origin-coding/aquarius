package com.origincoding.aquarius.iam.infrastructure.session

object RedisLoginSessionKeys {
    fun accessTokenKey(accessTokenHash: String): String =
        "aquarius:iam:session:access:$accessTokenHash"

    fun refreshTokenKey(refreshTokenHash: String): String =
        "aquarius:iam:session:refresh:$refreshTokenHash"

    fun sessionKey(sessionId: String): String =
        "aquarius:iam:session:record:$sessionId"

    fun userSessionsKey(userId: String): String =
        "aquarius:iam:session:user-sessions:$userId"
}
