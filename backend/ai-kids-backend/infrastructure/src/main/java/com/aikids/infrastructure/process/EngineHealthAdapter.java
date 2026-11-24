package com.aikids.infrastructure.process;

import com.aikids.application.port.out.ToolHealthPort;
import com.aikids.application.shared.ToolStatus;
import com.aikids.infrastructure.EngineSettings;
import com.aikids.infrastructure.script.ScriptJson;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Le pregunta al propio motor qué herramientas encuentra: así la respuesta nunca difiere de lo que usará. */
@Component
class EngineHealthAdapter implements ToolHealthPort {

    private final EngineSettings settings;
    private final ScriptJson json;

    EngineHealthAdapter(EngineSettings settings, ScriptJson json) {
        this.settings = settings;
        this.json = json;
    }

    @Override
    public List<ToolStatus> check() {
        List<ToolStatus> result = new ArrayList<>();
        boolean python = Files.isRegularFile(settings.python());
        result.add(new ToolStatus("Python", "Dirige el motor de producción", python, python ? "Listo" : "No está el entorno .venv"));
        if (python) {
            result.addAll(askEngine());
        }
        return result;
    }

    private List<ToolStatus> askEngine() {
        try {
            ProcessBuilder builder = new ProcessBuilder(settings.python().toString(), settings.producer().toString(), "--revisar");
            builder.environment().put("PYTHONUTF8", "1");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return List.of(failed("El motor no respondió a tiempo"));
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                return List.of(failed(output.lines().reduce((a, b) -> b).orElse("Sin respuesta")));
            }
            List<ToolStatus> tools = new ArrayList<>();
            for (JsonNode t : json.tree(output)) {
                tools.add(new ToolStatus(t.path("name").asString(""), t.path("purpose").asString(""), t.path("ready").asBoolean(false),
                        t.path("detail").asString("")));
            }
            return tools;
        } catch (IOException | RuntimeException e) {
            return List.of(failed(e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of(failed("Revisión interrumpida"));
        }
    }

    private static ToolStatus failed(String detail) {
        return new ToolStatus("Motor de producción", "Crea los videos", false, detail);
    }
}
