package com.aikids.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "production_job")
public class ProductionJobEntity {
    @Id
    public String id;
    public String episodeId;
    public String status;
    public String stage;
    public int progress;
    public String message;
    public String errorDetail;
    public long createdAt;
    public Long startedAt;
    public Long finishedAt;
}
