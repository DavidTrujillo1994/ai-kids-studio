package com.aikids.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "episode")
public class EpisodeEntity {
    @Id
    public String id;
    public String seriesId;
    public String folder;
    public String title;
    public String idea;
    public String environment;
    public String script;
    public String status;
    public long createdAt;
    public long updatedAt;
}
