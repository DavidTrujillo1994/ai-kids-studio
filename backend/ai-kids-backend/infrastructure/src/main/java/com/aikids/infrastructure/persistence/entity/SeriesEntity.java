package com.aikids.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "series")
public class SeriesEntity {
    @Id
    public String id;
    public String name;
    public String description;
    public long createdAt;
    public long updatedAt;
}
