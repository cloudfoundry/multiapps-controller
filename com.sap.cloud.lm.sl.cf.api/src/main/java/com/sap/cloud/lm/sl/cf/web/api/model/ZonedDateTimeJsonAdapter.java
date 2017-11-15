package com.sap.cloud.lm.sl.cf.web.api.model;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ZonedDateTimeJsonAdapter extends TypeAdapter<ZonedDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @Override
    public ZonedDateTime read(JsonReader in) throws IOException {
        if (!in.hasNext()) {
            return null;
        }
        return ZonedDateTime.parse(in.nextString(), FORMATTER);
    }

    @Override
    public void write(JsonWriter out, ZonedDateTime zonedDateTime) throws IOException {
        if (zonedDateTime == null) {
            out.nullValue();
        } else {
            out.value(FORMATTER.format(zonedDateTime));
        }
    }

}
