package com.learn.notification

import com.learn.post.Post
import com.learn.user.User
import jakarta.enterprise.context.ApplicationScoped
import org.jboss.logging.Logger

@ApplicationScoped
class NotificationService {

    private val log = Logger.getLogger(NotificationService::class.java)

    fun onPostCreated(post: Post) {
        val otherUsers = User.find("id != ?1", post.author.id).list()

        for (user in otherUsers) {
            Notification().apply {
                recipient = user
                actor = post.author
                type = NotificationType.NEW_POST
                title = "${post.author.username} published a new post"
                body = post.title
                referenceId = post.id
            }.persist()
        }

        log.info(
            "Created ${otherUsers.size} notifications for post id=${post.id} authorId=${post.author.id}",
        )
    }
}
