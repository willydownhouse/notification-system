package com.learn.post

import com.learn.notification.Notification
import com.learn.user.User
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import jakarta.transaction.Transactional
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.notNullValue
import org.jboss.logging.Logger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
class PostResourceTest {

    private val log = Logger.getLogger(PostResourceTest::class.java)

    @BeforeEach
    @Transactional
    fun beforeEach() {
        Notification.deleteAll()
        Post.deleteAll()
        User.deleteAll()
    }

    @Test
    fun `create and list posts`() {
        val authorId = given()
            .contentType(ContentType.JSON)
            .body("""{"username":"alice","email":"alice@example.com"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"$authorId","title":"Hello world","body":"My first post"}""")
            .post("/posts")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("authorId", equalTo(authorId))
            .body("title", equalTo("Hello world"))
            .body("body", equalTo("My first post"))
            .body("createdAt", notNullValue())

        given()
            .get("/posts")
            .then()
            .statusCode(200)
            .body("size()", equalTo(1))
            .body("[0].title", equalTo("Hello world"))
            .body("[0].authorId", equalTo(authorId))
            .body("[0].body", equalTo("My first post"))
    }

    @Test
    fun `reject unknown author`() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"${UUID.randomUUID()}","title":"Hello","body":"Body"}""")
            .post("/posts")
            .then()
            .statusCode(400)
            .body("status", equalTo(400))
            .body("message", equalTo("Invalid authorId"))
    }

    @Test
    fun `reject blank title`() {
        val authorId = given()
            .contentType(ContentType.JSON)
            .body("""{"username":"alice","email":"alice@example.com"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"$authorId","title":"","body":"Body"}""")
            .post("/posts")
            .then()
            .statusCode(400)
    }

    @Test
    fun `reject blank body`() {
        val authorId = given()
            .contentType(ContentType.JSON)
            .body("""{"username":"alice","email":"alice@example.com"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path<String>("id")

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"$authorId","title":"","body":""}""")
            .post("/posts")
            .then()
            .statusCode(400)
    }

    @Test
    fun `creating post notifies other users but not author`() {
        val aliceId = createUser("alice", "alice@example.com")
        val bobId = createUser("bob", "bob@example.com")

        given()
            .contentType(ContentType.JSON)
            .body("""{"authorId":"$aliceId","title":"Hello world","body":"My first post"}""")
            .post("/posts")
            .then()
            .statusCode(201)

        val notificationsResponse = given()
            .get("/notifications?userId=$bobId")
            .then()
            .statusCode(200)
            .extract()
            .response()

        log.info("Notifications: ${notificationsResponse.body.asString()}")

        notificationsResponse.then()
            .body("size()", equalTo(1))
            .body("[0].type", equalTo("NEW_POST"))
            .body("[0].title", equalTo("alice published a new post"))
            .body("[0].body", equalTo("Hello world"))
            .body("[0].read", equalTo(false))
            .body("[0].recipientId", equalTo(bobId))
            .body("[0].actorId", equalTo(aliceId))
    }

    private fun createUser(username: String, email: String): String =
        given()
            .contentType(ContentType.JSON)
            .body("""{"username":"$username","email":"$email"}""")
            .post("/users")
            .then()
            .statusCode(201)
            .extract()
            .path("id")
}
