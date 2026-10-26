package me.cedricrev.mseries.core.scheduler;

public interface Task {

    void cancel();

    boolean isCancelled();
}
