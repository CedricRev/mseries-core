package me.cedricrev.mseries.core.command;

import org.incendo.cloud.CommandManager;

interface CommandBackend {

    CommandManager<CommandActor> manager();
}
