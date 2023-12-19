package com.redhat.photogallery.like;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.MessageConsumer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

@QuarkusTest
public class LikeServiceTest {

    @Inject
    DataSource dataSource;

    @Inject
    EventBus eventBus;

    @BeforeEach
    public void setup() {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement truncateStatement = connection.prepareStatement(
                     "TRUNCATE TABLE LikesItem")) {
            truncateStatement.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldAddLikes() {
        // Given
        // language=json
        final String givenPayload = """
                {
                    "id": 1,
                    "likes": 6
                }
                """;

        // When & Then
        given()
                .body(givenPayload)
                .contentType(ContentType.JSON)
                .when()
                .post("/likes")
                .then()
                .statusCode(204);

        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement countPreparedStatement = connection.prepareStatement(
                     "SELECT COUNT(*) AS count FROM LikesItem");
             final PreparedStatement selectLikesPreparedStatement = connection.prepareStatement(
                     "SELECT * FROM LikesItem")) {
            final ResultSet countResultSet = countPreparedStatement.executeQuery();
            countResultSet.next();
            assertThat(countResultSet.getInt("count")).isEqualTo(1);

            final ResultSet likesResultSet = selectLikesPreparedStatement.executeQuery();
            likesResultSet.next();
            assertAll(
                    () -> assertThat(likesResultSet.getLong("id")).isEqualTo(1L),
                    () -> assertThat(likesResultSet.getInt("likes")).isEqualTo(6)
            );
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void shouldPublishMessageWhenAddingLikes() {
        // Given
        final MessageConsumer<JsonObject> givenTopic = eventBus.consumer("likes");
        final List<JsonObject> messagesConsumed = new ArrayList<>();
        givenTopic.handler(json -> messagesConsumed.add(json.body()));
        // language=json
        final String givenPayload = """
                {
                    "id": 1,
                    "likes": 6
                }
                """;

        // When
        given()
                .body(givenPayload)
                .contentType(ContentType.JSON)
                .when()
                .post("/likes")
                .then()
                .statusCode(204);

        // Then
        assertAll(
                () -> assertThat(messagesConsumed.get(0).getLong("id")).isEqualTo(1L),
                () -> assertThat(messagesConsumed.get(0).getInteger("likes")).isEqualTo(6));
    }

    @Test
    public void shouldReadAllLikes() {
        // Given
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement truncateStatement = connection.prepareStatement(
                     "INSERT INTO LikesItem(id, likes) VALUES (1, 6)")) {
            truncateStatement.execute();
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }

        // When & Then
        given()
                .get("/likes")
                .then()
                .statusCode(200)
                .body("size()", is(1))
                .body("[0].id", is(1))
                .body("[0].likes", is(6));
    }
}
