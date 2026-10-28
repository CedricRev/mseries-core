package me.cedricrev.mseries.core.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NamesTest {

    @Test
    void gamertagBecomesFloodgateName() {
        assertEquals("Cool_Gamer", Names.floodgateName("", "Cool Gamer"));
        assertEquals(".Cool_Gamer", Names.floodgateName(".", "Cool Gamer"));
    }

    @Test
    void floodgateNameIsCutToSixteen() {
        assertEquals("A_Very_Long_Game", Names.floodgateName("", "A Very Long Gamertag"));
        assertEquals(".A_Very_Long_Gam", Names.floodgateName(".", "A Very Long Gamertag"));
    }

    @Test
    void looseKeyIgnoresCaseSpacesAndUnderscores() {
        assertEquals(Names.looseKey("Cool_Gamer"), Names.looseKey("cool gamer"));
        assertEquals(Names.looseKey("CoolGamer"), Names.looseKey("Cool Gamer"));
        assertNotEquals(Names.looseKey("CoolGamer1"), Names.looseKey("CoolGamer"));
    }

    @Test
    void exactKeyOnlyIgnoresCase() {
        assertEquals(Names.exactKey("Steve"), Names.exactKey("sTEVE"));
        assertNotEquals(Names.exactKey("Cool_Gamer"), Names.exactKey("Cool Gamer"));
    }

    @Test
    void rejectsImplausibleInput() {
        assertTrue(Names.plausible("Cool Gamer"));
        assertTrue(Names.plausible("Steve_01"));
        assertFalse(Names.plausible(null));
        assertFalse(Names.plausible("   "));
        assertFalse(Names.plausible("drop;table"));
        assertFalse(Names.plausible("x".repeat(41)));
    }
}
