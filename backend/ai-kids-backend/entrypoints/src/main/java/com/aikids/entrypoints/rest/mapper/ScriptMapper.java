package com.aikids.entrypoints.rest.mapper;

import com.aikids.domain.script.CastMember;
import com.aikids.domain.script.PathPoint;
import com.aikids.domain.script.Script;
import com.aikids.domain.script.Step;
import com.aikids.domain.shared.AssetKey;
import com.aikids.entrypoints.rest.dto.Dtos.CastDto;
import com.aikids.entrypoints.rest.dto.Dtos.PointDto;
import com.aikids.entrypoints.rest.dto.Dtos.ScriptDto;
import com.aikids.entrypoints.rest.dto.Dtos.StepDto;
import com.aikids.entrypoints.rest.dto.Dtos.StepType;
import java.util.List;

public final class ScriptMapper {

    private ScriptMapper() {
    }

    public static Script toDomain(ScriptDto dto) {
        return new Script(dto.cast().stream().map(c -> new CastMember(new AssetKey(c.character()), c.startX())).toList(),
                or(dto.ambience(), Script.DEFAULT_AMBIENCE), dto.steps().stream().map(ScriptMapper::toDomain).toList());
    }

    public static ScriptDto toDto(Script script) {
        return new ScriptDto(script.cast().stream().map(c -> new CastDto(c.character().value(), c.startX())).toList(),
                script.ambience(), script.steps().stream().map(ScriptMapper::toDto).toList());
    }

    private static Step toDomain(StepDto s) {
        String expression = or(s.expression(), Step.DEFAULT_EXPRESSION);
        return switch (s.type()) {
            case WALK -> new Step.Walk(new AssetKey(s.character()), s.to() == null ? 0 : s.to(), or(s.gait(), Step.DEFAULT_GAIT), expression);
            case SAY -> new Step.Say(new AssetKey(s.character()), or(s.text(), "").strip(), or(s.animation(), "talk"), expression);
            case PLAY -> new Step.Play(new AssetKey(s.character()), or(s.animation(), "idle"), expression);
            case PAUSE -> new Step.Pause(new AssetKey(s.character()), s.seconds() == null ? 1 : s.seconds(), or(s.animation(), "idle"), expression);
            case SCENE -> new Step.Scene(new AssetKey(s.environment()), or(s.ambience(), Script.DEFAULT_AMBIENCE), s.positions() == null ? List.of()
                    : s.positions().stream().map(p -> new CastMember(new AssetKey(p.character()), p.startX())).toList());
            case APPEAR -> new Step.Appear(new AssetKey(s.prop()), s.path() == null ? List.of()
                    : s.path().stream().map(p -> new PathPoint(p.second(), p.x(), p.y())).toList());
        };
    }

    private static StepDto toDto(Step step) {
        return switch (step) {
            case Step.Walk s -> new StepDto(StepType.WALK, s.character().value(), null, null, s.expression(), s.to(), s.gait(), null, null, null, null, null, null);
            case Step.Say s -> new StepDto(StepType.SAY, s.character().value(), s.text(), s.animation(), s.expression(), null, null, null, null, null, null, null, null);
            case Step.Play s -> new StepDto(StepType.PLAY, s.character().value(), null, s.animation(), s.expression(), null, null, null, null, null, null, null, null);
            case Step.Pause s -> new StepDto(StepType.PAUSE, s.character().value(), null, s.animation(), s.expression(), null, null, s.seconds(), null, null, null, null, null);
            case Step.Appear s -> new StepDto(StepType.APPEAR, null, null, null, null, null, null, null, s.prop().value(),
                    s.path().stream().map(p -> new PointDto(p.second(), p.x(), p.y())).toList(), null, null, null);
            case Step.Scene s -> new StepDto(StepType.SCENE, null, null, null, null, null, null, null, null, null, s.environment().value(), s.ambience(),
                    s.positions().stream().map(p -> new CastDto(p.character().value(), p.startX())).toList());
        };
    }

    private static String or(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
