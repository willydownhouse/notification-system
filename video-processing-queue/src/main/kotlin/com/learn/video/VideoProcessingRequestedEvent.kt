package com.learn.video

import java.time.Instant
import java.util.UUID

data class VideoProcessingRequestedEvent(
    val jobId: UUID,
    val inputUrl: String,
    val createdAt: Instant,
)
