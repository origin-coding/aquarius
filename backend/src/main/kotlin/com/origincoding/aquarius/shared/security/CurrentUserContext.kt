package com.origincoding.aquarius.shared.security

import kotlinx.coroutines.ThreadContextElement
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext

object CurrentUserContext {
    private val currentUserHolder = ThreadLocal<CurrentUser?>()

    fun currentUser(): CurrentUser? = currentUserHolder.get()

    fun asContextElement(user: CurrentUser): ThreadContextElement<CurrentUser?> =
        currentUserHolder.asContextElement(user)

    fun <T> runAs(user: CurrentUser, block: () -> T): T {
        val previous = currentUserHolder.get()
        currentUserHolder.set(user)

        return try {
            block()
        } finally {
            if (previous == null) {
                currentUserHolder.remove()
            } else {
                currentUserHolder.set(previous)
            }
        }
    }

    suspend fun <T> runAsSuspend(user: CurrentUser, block: suspend () -> T): T =
        withContext(asContextElement(user)) {
            block()
        }
}
