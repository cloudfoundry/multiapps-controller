package com.sap.cloud.lm.sl.cf.web.api.model;

import java.io.IOException;
import java.time.ZonedDateTime;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class ZonedDateTimeJsonAdapter extends TypeAdapter<ZonedDateTime> {

    @Override
    public ZonedDateTime read(JsonReader in) throws IOException {
        return ZonedDateTime.parse(in.nextString(), Operation.DATE_TIME_FORMATTER);
    }

    @Override
    public void write(JsonWriter out, ZonedDateTime zonedDateTime) throws IOException {
        if(zonedDateTime != null) {
            out.value(Operation.DATE_TIME_FORMATTER.format(zonedDateTime));            
        } else {
            out.value(Constants.NULL_STRING_VALUE);
        }
    }

}
