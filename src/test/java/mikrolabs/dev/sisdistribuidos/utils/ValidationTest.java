package mikrolabs.dev.sisdistribuidos.utils;

import com.google.gson.JsonParser;
import mikrolabs.dev.sisdistribuidos.DTOs.Response;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test void nameBoundariesAndCharacters() {
        assertTrue(FieldValidation.validName("\u00c1"));
        assertTrue(FieldValidation.validName("Ana Maria"));
        assertTrue(FieldValidation.validName("a".repeat(60)));
        for (String value : new String[]{"", "   ", "a".repeat(61), "Ana1", "Ana-Maria", "Ana\nMaria"})
            assertFalse(FieldValidation.validName(value), value);
        assertFalse(FieldValidation.validName(null));
    }
    @Test void usernameBoundariesAndCharacters() {
        assertTrue(FieldValidation.validUsername("a._"));
        assertTrue(FieldValidation.validUsername("a".repeat(20)));
        for (String value : new String[]{"ab", "a".repeat(21), "Ana", "an\u00e1", " ana", "ana ", "ana-1"})
            assertFalse(FieldValidation.validUsername(value), value);
        assertFalse(FieldValidation.validUsername(null));
    }
    @Test void passwordRequiresAllCategoriesAndAllowedCharacters() {
        assertTrue(FieldValidation.validPassword("Abcdef1!"));
        assertTrue(FieldValidation.validPassword("Aa1!" + "a".repeat(16)));
        for (char symbol : "#.*&%$@!()-_=+".toCharArray())
            assertTrue(FieldValidation.validPassword("Abcdef1" + symbol));
        for (String value : new String[]{"Abcde1!", "Aa1!" + "a".repeat(17), "abcdef1!", "ABCDEF1!", "Abcdefg!", "Abcdef12", "Abcdef1?", "Abcdef1 ", "\u00c1bcdef1!"})
            assertFalse(FieldValidation.validPassword(value), value);
        assertFalse(FieldValidation.validPassword(null));
    }
    @Test void tokenMustBeAUuidString() {
        String token = "4f53ec56-f43e-4a68-8829-174e682d9aa7";
        assertEquals(token, LoginResponseValidation.extractToken(new Response(200, "ok",
                JsonParser.parseString("{\"token\":\"" + token + "\"}"))));
        for (String data : new String[]{"null", "[]", "{}", "{\"token\":null}", "{\"token\":123}", "{\"token\":\"\"}", "{\"token\":\"invalid\"}", "{\"token\":\"1-1-1-1-1\"}"})
            assertThrows(IllegalArgumentException.class, () -> LoginResponseValidation.extractToken(
                    new Response(200, "ok", JsonParser.parseString(data))), data);
        assertThrows(IllegalArgumentException.class, () -> LoginResponseValidation.extractToken(new Response(200, "ok", null)));
    }
}
