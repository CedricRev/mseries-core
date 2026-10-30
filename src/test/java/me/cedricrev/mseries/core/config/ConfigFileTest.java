package me.cedricrev.mseries.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class ConfigFileTest {

    private static final String DEFAULTS = """
            config-version: 2
            sites:
              example:
                name: Example
                url: https://example.org
            rewards:
              "3":
                money: 10
            options:
              enabled: true
              delay: 5
            """;

    private static YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }

    @Test
    void addsMissingSettings() throws InvalidConfigurationException {
        YamlConfiguration target = yaml("options:\n  enabled: false\n");

        ConfigFile.addMissing(target, yaml(DEFAULTS), List.of());

        assertFalse(target.getBoolean("options.enabled"));
        assertEquals(5, target.getInt("options.delay"));
        assertTrue(target.contains("sites.example.url"));
    }

    @Test
    void leavesOpenSectionsAlone() throws InvalidConfigurationException {
        YamlConfiguration target = yaml("sites:\n  mine:\n    name: Mine\nrewards: {}\n");

        ConfigFile.addMissing(target, yaml(DEFAULTS), List.of("sites", "rewards"));

        assertEquals(List.of("mine"), List.copyOf(target.getConfigurationSection("sites").getKeys(false)));
        assertTrue(target.getConfigurationSection("rewards").getKeys(false).isEmpty());
        assertEquals(5, target.getInt("options.delay"));
    }

    @Test
    void fillsOpenSectionsThatAreMissing() throws InvalidConfigurationException {
        YamlConfiguration target = yaml("options:\n  enabled: true\n");

        ConfigFile.addMissing(target, yaml(DEFAULTS), List.of("sites"));

        assertEquals("https://example.org", target.getString("sites.example.url"));
    }
}
