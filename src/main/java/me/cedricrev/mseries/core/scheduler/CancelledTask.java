package me.cedricrev.mseries.core.scheduler;

enum CancelledTask implements Task {
    INSTANCE;

    @Override
    public void cancel() {
    }

    @Override
    public boolean isCancelled() {
        return true;
    }
}
