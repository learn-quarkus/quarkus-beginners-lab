package org.coffee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class MenuItem extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    public String description;

    @Column(nullable = false)
    public double price;

    public MenuItem() {}
}
