package mikrolabs.dev.sisdistribuidos.utils;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileResponseValidationTest {
    @Test
    void readsTextFieldsAndIgnoresAdditionalFields() {
        var profile = ProfileResponseValidation.readData(JsonParser.parseString(
                "{\"name\":\"Ana Maria\",\"username\":\"ana\",\"extra\":42}"));

        assertNotNull(profile);
        assertEquals("Ana Maria", profile.name());
        assertEquals("ana", profile.username());
    }

    @Test
    void acceptsPartialData() {
        var onlyName = ProfileResponseValidation.readData(JsonParser.parseString("{\"name\":\"Ana\"}"));
        var onlyUsername = ProfileResponseValidation.readData(JsonParser.parseString("{\"username\":\"ana\"}"));

        assertNotNull(onlyName);
        assertEquals("Ana", onlyName.name());
        assertNull(onlyName.username());
        assertNotNull(onlyUsername);
        assertNull(onlyUsername.name());
        assertEquals("ana", onlyUsername.username());
    }

    @Test
    void invalidFieldsAreIgnoredWithoutCoercingNumbersOrBooleansToText() {
        for (String invalid : new String[]{"null", "123", "true", "{}", "[]", "\"\"", "\"   \""}) {
            var invalidName = ProfileResponseValidation.readData(JsonParser.parseString(
                    "{\"name\":" + invalid + ",\"username\":\"ana\"}"));
            var invalidUsername = ProfileResponseValidation.readData(JsonParser.parseString(
                    "{\"name\":\"Ana\",\"username\":" + invalid + "}"));

            assertNotNull(invalidName, invalid);
            assertNull(invalidName.name(), invalid);
            assertEquals("ana", invalidName.username(), invalid);
            assertNotNull(invalidUsername, invalid);
            assertEquals("Ana", invalidUsername.name(), invalid);
            assertNull(invalidUsername.username(), invalid);
        }
    }

    @Test
    void unusableDataReturnsNullWithoutThrowing() {
        assertNull(ProfileResponseValidation.readData(null));
        for (String data : new String[]{
                "null", "[]", "42", "true", "\"texto\"", "{}",
                "{\"name\":null,\"username\":123}",
                "{\"name\":\"   \",\"username\":\"\"}"
        }) {
            assertNull(ProfileResponseValidation.readData(JsonParser.parseString(data)), data);
        }
    }

    @Test
    void preservesTextWithoutApplyingRegistrationRules() {
        var profile = ProfileResponseValidation.readData(JsonParser.parseString(
                "{\"name\":\"  Ana-Maria  \",\"username\":\" USUÁRIO! \"}"));

        assertNotNull(profile);
        assertEquals("  Ana-Maria  ", profile.name());
        assertEquals(" USUÁRIO! ", profile.username());
    }
}
