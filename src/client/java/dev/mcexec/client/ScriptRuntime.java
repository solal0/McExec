package dev.mcexec.client;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import net.minecraft.client.Minecraft;
import org.codehaus.groovy.control.CompilerConfiguration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Groovy compilation off the render thread; execution and callbacks on Minecraft's client thread. */
public final class ScriptRuntime {
    private final Minecraft mc = Minecraft.getInstance();
    private final ScriptAPI api;
    private final ExecutorService compiler = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "McExec compiler");
        thread.setDaemon(true);
        thread.setContextClassLoader(MCExecClient.class.getClassLoader());
        return thread;
    });
    private final ThreadLocal<ScriptSession> current = new ThreadLocal<>();
    private final Map<String, ScriptSession> sessions = new LinkedHashMap<>();
    public enum LogLevel { SYSTEM, SCRIPT, EXECUTION, SUCCESS, WARNING, ERROR }
    public record LogEntry(String text, LogLevel level) {}
    private final List<LogEntry> lines = new ArrayList<>();
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private long epoch;
    private long requestId;
    private long nextId;
    private volatile boolean busy;

    ScriptRuntime(ScriptAPI api) { this.api = api; log("McExec ready. Scripts run with your full JVM privileges."); }
    public boolean busy() { return busy; }
    public ScriptSession currentSession() { return current.get(); }
    ScriptSession requireSession() {
        ScriptSession session = current.get();
        if (session == null || !session.active())
            throw new IllegalStateException("This operation needs an active McExec script session");
        return session;
    }
    public List<String> snapshot() { synchronized (lines) { return lines.stream().map(LogEntry::text).toList(); } }
    public List<LogEntry> snapshotColored() { synchronized (lines) { return List.copyOf(lines); } }
    public String allLogs() { synchronized (lines) { return String.join("\n", lines.stream().map(LogEntry::text).toList()); } }
    public void clearLog() { synchronized (lines) { lines.clear(); } }
    public void log(String message) { append(current.get() == null ? LogLevel.SYSTEM : LogLevel.SCRIPT, message); }
    public void warn(String message) { append(LogLevel.WARNING, message); }
    private void append(LogLevel level, String message) {
        synchronized (lines) {
            String time = LocalTime.now().format(TIME);
            for (String part : message.split("\\R", -1)) lines.add(new LogEntry("[" + time + "] " + part, level));
            while (lines.size() > 1500) lines.removeFirst();
        }
    }
    public void error(String label, Throwable cause) {
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));
        append(LogLevel.ERROR, label + "\n" + sw);
    }

    /** Calls a callback inside the owning session so registrations and undo actions remain scoped. */
    void invoke(ScriptSession session, Closure<?> callback, Object arg) {
        if (!session.active()) return;
        ScriptSession previous = current.get();
        current.set(session);
        try { callback.call(arg); }
        finally {
            if (previous == null) current.remove(); else current.set(previous);
        }
    }

    public void execute(String source) { execute(source, "editor"); }
    public void execute(String source, String scriptName) {
        if (busy) { warn("Another script is compiling or executing"); return; }
        if (scriptName == null || scriptName.isBlank()) scriptName = "editor";
        final String name = scriptName;
        final long requestEpoch = epoch;
        final long request = ++requestId;
        busy = true;
        append(LogLevel.EXECUTION, "Compiling " + name + "...");
        CompletableFuture.supplyAsync(() -> {
            CompilerConfiguration config = new CompilerConfiguration();
            GroovyShell shell = new GroovyShell(MCExecClient.class.getClassLoader(), new Binding(), config);
            return shell.parse("""
                    import net.minecraft.client.Minecraft
                    import net.minecraft.network.chat.Component
                    import net.minecraft.core.*
                    import net.minecraft.resources.*
                    import net.minecraft.world.phys.*
                    """ + source, name + ".groovy");
        }, compiler).whenComplete((script, failure) -> mc.execute(() -> {
            if (requestEpoch != epoch || request != requestId) {
                warn("Discarded cancelled compilation: " + name);
                return;
            }
            if (failure != null) {
                error("Compilation failed: " + name, failure.getCause() == null ? failure : failure.getCause());
                busy = false;
                return;
            }
            ScriptSession old = sessions.remove(name);
            if (old != null) old.stop();
            ScriptSession session = new ScriptSession(name, ++nextId, mc.level);
            sessions.put(name, session);
            Binding bindings = new Binding();
            bindings.setVariable("mc", mc);
            bindings.setVariable("player", mc.player);
            bindings.setVariable("world", mc.level);
            bindings.setVariable("api", api);
            bindings.setVariable("draw", api.draw());
            bindings.setVariable("session", session);
            bindings.setVariable("log", new Closure<Object>(this) {
                public Object doCall(Object value) { log(String.valueOf(value)); return null; }
            });
            script.setBinding(bindings);
            ScriptSession previous = current.get();
            current.set(session);
            try {
                script.run();
                append(LogLevel.SUCCESS, "Executed " + name);
            } catch (Throwable t) {
                error("Script failed: " + name, t);
                // Roll back partial registrations when a script fails.
                sessions.remove(name);
                session.stop();
            } finally {
                if (previous == null) current.remove(); else current.set(previous);
                busy = false;
            }
        }));
    }

    /** Stops all managed script sessions, invalidating pending compilations. */
    public void stopAll() {
        epoch++;
        requestId++;
        busy = false;
        for (ScriptSession session : new ArrayList<>(sessions.values())) session.stop();
        sessions.clear();
        api.resetManagedState();
        append(LogLevel.WARNING, "Stop All: managed callbacks, rendering, movement, glow and textures reset.");
        warn("Raw JVM changes and network actions cannot be undone.");
    }
    public void stopScript(String name) {
        ScriptSession session = sessions.remove(name);
        if (session != null) { session.stop(); append(LogLevel.WARNING, "Stopped " + name); }
    }
    public int activeScripts() { return (int) sessions.values().stream().filter(ScriptSession::active).count(); }
    public void stopForWorldChange() {
        if (sessions.values().stream().anyMatch(s -> s.initialWorld() != null && s.initialWorld() != mc.level)) {
            warn("World changed: stopping world-bound scripts");
            stopAll();
        }
    }
}
