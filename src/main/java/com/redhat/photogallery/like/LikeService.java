package com.redhat.photogallery.like;

import com.redhat.photogallery.common.Constants;
import com.redhat.photogallery.common.data.LikesAddedMessage;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.MessageProducer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.GenericEntity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

@Path("/likes")
public class LikeService {
    private static final Logger LOG = LoggerFactory.getLogger(LikeService.class);
    private final MessageProducer<JsonObject> topic;
    private final EntityManager entityManager;

    public LikeService(final EntityManager entityManager, final EventBus eventBus) {
        this.entityManager = Objects.requireNonNull(entityManager);
        this.topic = eventBus.publisher(Constants.LIKES_TOPIC_NAME);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    public void addLikes(final LikesItem item) {
        LikesItem savedItem = entityManager.find(LikesItem.class, item.id);
        if (savedItem == null) {
            item.persist();
            savedItem = item;
        } else {
            final int likes = savedItem.likes + item.likes;
            savedItem.likes = likes;
        }
        LOG.info("Updated in data store {}", savedItem);

        final LikesAddedMessage message = new LikesAddedMessage(savedItem.id, savedItem.likes);
        topic.write(JsonObject.mapFrom(message))
                .subscribe()
                .with(onItemCallback -> LOG.info("Published {} update on topic {}", message, topic.address()));
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response readAllLikes() {
        final TypedQuery<LikesItem> query = entityManager.createQuery("FROM LikesItem", LikesItem.class);
        final List<LikesItem> items = query.getResultList();
        LOG.info("Returned all {} items", items.size());
        return Response.ok(new GenericEntity<>(items) {
        }).build();
    }
}