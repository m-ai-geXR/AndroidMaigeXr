package com.xraiassistant.data.local.entities

import androidx.room.Embedded

/**
 * A conversation with the two things the history list shows beside it: how many
 * messages it holds and the first reply, both computed in one query rather than
 * loading every message.
 */
data class ConversationSummary(
    @Embedded val conversation: ConversationEntity,
    val messageCount: Int,
    val firstReply: String?
)
