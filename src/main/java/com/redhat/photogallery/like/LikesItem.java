package com.redhat.photogallery.like;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class LikesItem extends PanacheEntityBase {

    @Id
    public Long id;
    public int likes;

    @Override
    public String toString() {
        return "LikesItem [id=" + id + ", likes=" + likes + "]";
    }

}
