package com.zainabtaha.banking.repository.file;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void roundTripsFieldsWithCommasAndQuotes() {
        List<String> fields = List.of("plain", "Doe, Jane", "say \"hi\"", "");
        assertEquals(fields, Csv.parseRow(Csv.formatRow(fields)));
    }

    @Test
    void onlyQuotesWhenNeeded() {
        assertEquals("a,\"b,c\",d", Csv.formatRow(List.of("a", "b,c", "d")));
    }

    @Test
    void parsesEmptyTrailingField() {
        assertEquals(List.of("a", "b", ""), Csv.parseRow("a,b,"));
    }
}
