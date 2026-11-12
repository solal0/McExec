package dev.mcexec.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** One user script execution and every McExec-owned resource it registered. */
public final class ScriptSession {
    private final String name;
    private final long id;
    private final Object initialWorld;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final List<Runnable> cleanup = new ArrayList<>();

    ScriptSession(String name, long id, Object initialWorld) {
        this.name = name;
        this.id = id;
        this.initialWorld = initialWorld;
    }
    public String name() { return name; }
    public long id() { return id; }
    public boolean active() { return active.get(); }
    public boolean cancelled() { return !active(); }
    public Object initialWorld() { return initialWorld; }

    /** Registers an undo operation. Only McExec-owned modifications can be tracked. */
    public synchronized void onStop(Runnable action) {
        if (!active.get()) { action.run(); return; }
        cleanup.add(action);
    }

    /** Idempotent, best-effort, reverse-order resource cleanup. Must run on the client thread. */
    public void stop() {
        if (!active.compareAndSet(true, false)) return;
        List<Runnable> actions;
        synchronized (this) {
            actions = new ArrayList<>(cleanup);
            cleanup.clear();
        }
        for (int i = actions.size() - 1; i >= 0; i--) {
            try { actions.get(i).run(); }
            catch (Throwable e) { MCExecClient.runtime().error("Cleanup failed in " + name, e); }
        }
    }
}
