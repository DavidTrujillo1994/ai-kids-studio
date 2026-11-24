package com.aikids.domain.character;

import com.aikids.domain.series.SeriesId;
import com.aikids.domain.shared.AssetKey;
import com.aikids.domain.shared.InvalidStateException;
import com.aikids.domain.shared.Text;

/** Personaje de una serie. Su arte vive en disco; aquí viven su ficha y la aprobación del diseño. */
public final class StoryCharacter {

    private final CharacterId id;
    private final SeriesId seriesId;
    private final AssetKey assetKey;
    private final CharacterRole role;
    private String name;
    private String personality;
    private int designVersion;
    private DesignStatus designStatus;

    public StoryCharacter(CharacterId id, SeriesId seriesId, AssetKey assetKey, CharacterRole role, String name,
                          String personality, int designVersion, DesignStatus designStatus) {
        this.id = id;
        this.seriesId = seriesId;
        this.assetKey = assetKey;
        this.role = role;
        this.designVersion = designVersion;
        this.designStatus = designStatus;
        assign(name, personality);
    }

    public static StoryCharacter register(SeriesId seriesId, AssetKey assetKey, CharacterRole role, String name,
                                          String personality, int designVersion) {
        return new StoryCharacter(CharacterId.create(), seriesId, assetKey, role, name, personality, designVersion,
                DesignStatus.BORRADOR);
    }

    public void updateProfile(String newName, String newPersonality) {
        if (designStatus == DesignStatus.APROBADO) {
            throw new InvalidStateException(name + " tiene el diseño aprobado y está bloqueado.");
        }
        assign(newName, newPersonality);
    }

    public void approveDesign() {
        if (designStatus == DesignStatus.APROBADO) {
            throw new InvalidStateException("El diseño de " + name + " ya estaba aprobado.");
        }
        designStatus = DesignStatus.APROBADO;
    }

    /** Un cambio de arte llega como versión nueva y exige aprobarse otra vez. */
    public boolean detectDesign(int currentVersion) {
        if (currentVersion == designVersion) {
            return false;
        }
        designVersion = currentVersion;
        designStatus = DesignStatus.BORRADOR;
        return true;
    }

    private void assign(String newName, String newPersonality) {
        name = Text.required(newName, "el nombre del personaje", 40);
        personality = Text.optional(newPersonality, "la personalidad", 300);
    }

    public CharacterId id() {
        return id;
    }

    public SeriesId seriesId() {
        return seriesId;
    }

    public AssetKey assetKey() {
        return assetKey;
    }

    public CharacterRole role() {
        return role;
    }

    public String name() {
        return name;
    }

    public String personality() {
        return personality;
    }

    public int designVersion() {
        return designVersion;
    }

    public DesignStatus designStatus() {
        return designStatus;
    }
}
