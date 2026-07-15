package com.learn.video

import jakarta.enterprise.context.ApplicationScoped
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.jboss.logging.Logger

@ApplicationScoped
class VideoJobService(
    @Channel("video-processing-requested-out")
    private val processingRequestedEmitter: Emitter<VideoProcessingRequestedEvent>,
) {

    private val log = Logger.getLogger(VideoJobService::class.java)

    fun onJobCreated(job: VideoJob) {
        val event = job.toProcessingRequestedEvent()
        processingRequestedEmitter.send(event)

        log.info("Published VideoProcessingRequestedEvent for job id=${event.jobId}")
    }
}
