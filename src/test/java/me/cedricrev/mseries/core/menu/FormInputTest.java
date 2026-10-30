package me.cedricrev.mseries.core.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class FormInputTest {

    private static final Field DIRECTION = Field.choice("direction", Component.text("Direction"))
            .option("SELL", Component.text("Sell"))
            .option("BUY", Component.text("Buy"));
    private static final Field BUNDLE = Field.number("bundle", Component.text("Bundle")).range(1, 64).initial(1);
    private static final Field NAME = Field.text("name", Component.text("Name")).range(1, 16);
    private static final Field PUBLIC = Field.toggle("public", Component.text("Public"));

    @Test
    void numberMustBeAWholeNumberInRange() {
        assertEquals(Optional.of(4), FormInput.check(BUNDLE, " 4 "));
        assertEquals(Optional.of(64), FormInput.check(BUNDLE, "+64"));
        assertTrue(FormInput.check(BUNDLE, "0").isEmpty());
        assertTrue(FormInput.check(BUNDLE, "65").isEmpty());
        assertTrue(FormInput.check(BUNDLE, "4.5").isEmpty());
        assertTrue(FormInput.check(BUNDLE, "").isEmpty());
        assertTrue(FormInput.check(BUNDLE, "99999999999999").isEmpty());
    }

    @Test
    void numberWithoutRangeAcceptsAnyInt() {
        Field price = Field.number("price", Component.text("Price"));
        assertEquals(Optional.of(-5), FormInput.check(price, "-5"));
        assertEquals(Optional.of(Integer.MAX_VALUE), FormInput.check(price, "2147483647"));
        assertTrue(FormInput.check(price, "2147483648").isEmpty());
    }

    @Test
    void choiceMatchesIdOrLabelIgnoringCase() {
        assertEquals(Optional.of("SELL"), FormInput.check(DIRECTION, "sell"));
        assertEquals(Optional.of("BUY"), FormInput.check(DIRECTION, "Buy"));
        assertTrue(FormInput.check(DIRECTION, "trade").isEmpty());
    }

    @Test
    void firstChoiceIsTheDefault() {
        assertEquals("SELL", DIRECTION.initial());
        assertEquals("BUY", DIRECTION.initial("BUY").initial());
    }

    @Test
    void textLengthIsChecked() {
        assertEquals(Optional.of("Spawn"), FormInput.check(NAME, "  Spawn "));
        assertTrue(FormInput.check(NAME, " ").isEmpty());
        assertTrue(FormInput.check(NAME, "a name that is far too long").isEmpty());
    }

    @Test
    void toggleAcceptsCommonWords() {
        assertEquals(Optional.of(true), FormInput.check(PUBLIC, "YES"));
        assertEquals(Optional.of(false), FormInput.check(PUBLIC, "off"));
        assertTrue(FormInput.check(PUBLIC, "maybe").isEmpty());
    }

    @Test
    void parseReturnsTheFirstInvalidField() {
        List<Field> fields = List.of(DIRECTION, BUNDLE, NAME);
        FormInput.Result result = FormInput.parse(fields, Map.of("direction", "BUY", "bundle", "100", "name", ""));
        assertFalse(result.valid());
        assertSame(BUNDLE, result.invalid());
    }

    @Test
    void parseBuildsTypedResponse() {
        List<Field> fields = List.of(DIRECTION, BUNDLE, NAME, PUBLIC);
        FormInput.Result result = FormInput.parse(fields,
                Map.of("direction", "buy", "bundle", "16", "name", "Shop", "public", "true"));
        assertTrue(result.valid());
        assertEquals("BUY", result.response().choice("direction"));
        assertEquals(16, result.response().number("bundle"));
        assertEquals("Shop", result.response().text("name"));
        assertTrue(result.response().toggle("public"));
        assertThrows(IllegalArgumentException.class, () -> result.response().text("missing"));
    }

    @Test
    void splitKeepsEmptyPartsAndSpaces() {
        String joined = "0" + FormInput.SEPARATOR + "4" + FormInput.SEPARATOR + "my shop";
        assertEquals(Optional.of(List.of("0", "4", "my shop")), FormInput.split(joined, 3));
        assertEquals(Optional.of(List.of("", "")), FormInput.split(String.valueOf(FormInput.SEPARATOR), 2));
        assertEquals(Optional.of(List.of("")), FormInput.split("", 1));
        assertTrue(FormInput.split("a" + FormInput.SEPARATOR + "b", 3).isEmpty());
    }

    @Test
    void formNeedsFieldsAndUniqueKeys() {
        assertThrows(IllegalStateException.class, () -> Menu.form(Component.text("Empty")).build());
        assertThrows(IllegalArgumentException.class, () -> Menu.form(Component.text("Twice"))
                .field(BUNDLE).field(BUNDLE));
        assertThrows(IllegalArgumentException.class, () -> Menu.form(Component.text("No options"))
                .field(Field.choice("x", Component.text("X"))));
        assertThrows(IllegalArgumentException.class, () -> BUNDLE.range(5, 1));
    }
}
