package com.aikids.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "story_character")
public class CharacterEntity {
    @Id
    public String id;
    public String seriesId;
    public String assetKey;
    public String role;
    public String name;
    public String personality;
    public int designVersion;
    public String designStatus;
}
