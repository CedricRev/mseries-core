package me.cedricrev.mseries.core.platform;

public final class Platform {

    private static final String FOLIA_MARKER = "io.papermc.paper.threadedregions.RegionizedServer";
    private static final String PURPUR_MARKER = "org.purpurmc.purpur.PurpurConfig";
    private static final String[] PAPER_MARKERS = {
            "io.papermc.paper.ServerBuildInfo",
            "io.papermc.paper.configuration.Configuration",
            "com.destroystokyo.paper.PaperConfig"
    };
    private static final String PAPER_COMMANDS_MARKER = "io.papermc.paper.command.brigadier.CommandSourceStack";

    private static final Platform CURRENT = new Platform();

    private final ServerCore core;
    private final boolean paperCommands;

    private Platform() {
        this.core = detectCore();
        this.paperCommands = this.core.isPaperBased() && classExists(PAPER_COMMANDS_MARKER);
    }

    public static Platform current() {
        return CURRENT;
    }

    public ServerCore core() {
        return this.core;
    }

    public boolean isPaperBased() {
        return this.core.isPaperBased();
    }

    public boolean isFolia() {
        return this.core == ServerCore.FOLIA;
    }

    public boolean hasPaperCommands() {
        return this.paperCommands;
    }

    @Override
    public String toString() {
        return this.core.name();
    }

    private static ServerCore detectCore() {
        if (classExists(FOLIA_MARKER)) {
            return ServerCore.FOLIA;
        }
        if (classExists(PURPUR_MARKER)) {
            return ServerCore.PURPUR;
        }
        for (String marker : PAPER_MARKERS) {
            if (classExists(marker)) {
                return ServerCore.PAPER;
            }
        }
        return ServerCore.SPIGOT;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, Platform.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }
}
