package com.origincoding.aquarius.iam.infrastructure.session

import com.origincoding.aquarius.iam.application.session.LoginSessionRevoker
import com.origincoding.aquarius.iam.application.session.RevokeAllLoginSessionsCommand
import com.origincoding.aquarius.iam.application.session.RevokeLoginSessionCommand
import org.springframework.stereotype.Component

@Component
class RedisLoginSessionRevoker(
    private val sessionStore: RedisLoginSessionStore,
    private val tokenHasher: TokenHasher,
) : LoginSessionRevoker {
    override fun revoke(command: RevokeLoginSessionCommand): Boolean {
        val accessTokenHash = tokenHasher.hash(command.accessToken)
        val accessTokenBucket = sessionStore.accessTokenBucket(accessTokenHash)
        val sessionId = accessTokenBucket.get() ?: return false

        val sessionBucket = sessionStore.sessionBucket(sessionId)
        val record = sessionBucket.get()

        accessTokenBucket.delete()
        if (record == null) {
            return false
        }

        revokeRecord(record)

        return true
    }

    override fun revokeAll(command: RevokeAllLoginSessionsCommand) {
        val userSessions = sessionStore.userSessions(command.userId)
        val sessionIds = userSessions.readAll()

        sessionIds.forEach { sessionId ->
            val sessionBucket = sessionStore.sessionBucket(sessionId)
            val record = sessionBucket.get()

            if (record != null) {
                revokeRecord(record)
            } else {
                sessionBucket.delete()
            }
        }

        userSessions.delete()
    }

    private fun revokeRecord(record: RedisLoginSessionRecord) {
        sessionStore.accessTokenBucket(record.accessTokenHash).delete()
        sessionStore.refreshTokenBucket(record.refreshTokenHash).delete()
        sessionStore.sessionBucket(record.id).delete()
        sessionStore.userSessions(record.userId).remove(record.id)
    }
}
