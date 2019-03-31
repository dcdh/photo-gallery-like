package com.redhat.photogallery.like;

import com.redhat.photogallery.common.Server;
import com.redhat.photogallery.common.VertxInit;

public class LikeServer {

    private static final int LISTEN_PORT = 8081;

    public static void main(String[] args) {
        VertxInit.createClusteredVertx(vertx -> {
            vertx.deployVerticle(new Server(LISTEN_PORT, new LikeComponent()));
        });
    }
}
