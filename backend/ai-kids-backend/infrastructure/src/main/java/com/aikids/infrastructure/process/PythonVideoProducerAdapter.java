package com.aikids.infrastructure.process;

import com.aikids.application.port.out.VideoProducerPort;
import com.aikids.domain.episode.Episode;
import com.aikids.domain.production.JobId;
import com.aikids.domain.production.Stage;
import com.aikids.infrastructure.EngineSettings;
import com.aikids.infrastructure.script.ScriptJson;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

/** Ejecuta el motor producir.py como proceso aparte, sin pasar texto del usuario por la línea de comandos. */
@Component
class PythonVideoProducerAdapter implements VideoProducerPort {

    private static final Logger LOG = LoggerFactory.getLogger(PythonVideoProducerAdapter.class);
    private static final String EVENT = "@@";
    private static final int KEPT_LINES = 40;

    private final EngineSettings settings;
    private final ScriptJson json;
    private final Map<JobId, Process> running = new ConcurrentHashMap<>();
    private final Set<JobId> cancelled = ConcurrentHashMap.newKeySet();

    PythonVideoProducerAdapter(EngineSettings settings, ScriptJson json) {
        this.settings = settings;
        this.json = json;
    }

    @Override
    public void produce(JobId jobId, Episode episode, ProgressListener listener) throws ProductionFailedException, ProductionCancelledException {
        Deque<String> tail = new ArrayDeque<>();
        try {
            writeScript(episode);
            Process process = start(episode.folder().value());
            running.put(jobId, process);
            if (cancelled.contains(jobId)) {
                kill(process);
            }
            read(process, listener, tail);
            int exit = process.waitFor();
            if (cancelled.contains(jobId)) {
                throw new ProductionCancelledException();
            }
            if (exit != 0) {
                throw new ProductionFailedException(summary(tail), String.join("\n", tail));
            }
        } catch (IOException e) {
            LOG.error("Falló el motor de producción", e);
            throw new ProductionFailedException("No se pudo ejecutar el motor de producción. Revisa los Ajustes.", e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProductionCancelledException();
        } finally {
            Process process = running.remove(jobId);
            if (process != null && process.isAlive()) {
                kill(process);
            }
            cancelled.remove(jobId);
        }
    }

    @Override
    public void cancel(JobId jobId) {
        cancelled.add(jobId);
        Process process = running.get(jobId);
        if (process != null) {
            kill(process);
        }
    }

    @PreDestroy
    void stopAll() {
        running.values().forEach(PythonVideoProducerAdapter::kill);
    }

    private void writeScript(Episode episode) throws IOException {
        Path file = settings.media().resolve("episodes").resolve(episode.folder().value()).resolve("script/guion.json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, json.writeForEngine(episode));
    }

    private Process start(String folder) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(settings.python().toString(), settings.producer().toString(), folder, "--eventos",
                "--padre=" + ProcessHandle.current().pid());
        builder.directory(settings.root().toFile());
        builder.redirectErrorStream(true);
        builder.environment().put("PYTHONUTF8", "1");
        builder.environment().put("PYTHONUNBUFFERED", "1");
        return builder.start();
    }

    private void read(Process process, ProgressListener listener, Deque<String> tail) throws IOException {
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(EVENT)) {
                    JsonNode event = json.tree(line.substring(EVENT.length()));
                    listener.onProgress(Stage.valueOf(event.path("stage").asString("")), event.path("progress").asInt(0),
                            event.path("message").asString(""));
                } else if (!line.isBlank()) {
                    tail.addLast(line);
                    if (tail.size() > KEPT_LINES) {
                        tail.removeFirst();
                    }
                }
            }
        }
    }

    /** Los mensajes pensados para la persona salen con el prefijo "AVISO:". */
    private static String summary(Deque<String> tail) {
        return tail.stream().filter(l -> l.startsWith("AVISO:")).reduce((a, b) -> b).map(l -> l.substring(6).strip())
                .orElse("El motor se detuvo con un error. Abre el detalle técnico para verlo.");
    }

    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}
