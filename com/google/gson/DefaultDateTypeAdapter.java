package com.google.gson;

import com.google.gson.internal.bind.util.ISO8601Utils;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class DefaultDateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {
    private final DateFormat enUsFormat;
    private final DateFormat localFormat;

    DefaultDateTypeAdapter() {
        this(DateFormat.getDateTimeInstance(2, 2, Locale.US), DateFormat.getDateTimeInstance(2, 2));
    }

    DefaultDateTypeAdapter(String datePattern) {
        this((DateFormat) new SimpleDateFormat(datePattern, Locale.US), (DateFormat) new SimpleDateFormat(datePattern));
    }

    DefaultDateTypeAdapter(int style) {
        this(DateFormat.getDateInstance(style, Locale.US), DateFormat.getDateInstance(style));
    }

    public DefaultDateTypeAdapter(int dateStyle, int timeStyle) {
        this(DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.US), DateFormat.getDateTimeInstance(dateStyle, timeStyle));
    }

    DefaultDateTypeAdapter(DateFormat enUsFormat2, DateFormat localFormat2) {
        this.enUsFormat = enUsFormat2;
        this.localFormat = localFormat2;
    }

    public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
        JsonPrimitive jsonPrimitive;
        synchronized (this.localFormat) {
            jsonPrimitive = new JsonPrimitive(this.enUsFormat.format(src));
        }
        return jsonPrimitive;
    }

    public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json instanceof JsonPrimitive) {
            Date date = deserializeToDate(json);
            if (typeOfT == Date.class) {
                return date;
            }
            if (typeOfT == Timestamp.class) {
                return new Timestamp(date.getTime());
            }
            if (typeOfT == java.sql.Date.class) {
                return new java.sql.Date(date.getTime());
            }
            throw new IllegalArgumentException(getClass() + " cannot deserialize to " + typeOfT);
        }
        throw new JsonParseException("The date should be a string value");
    }

    /* Debug info: failed to restart local var, previous not found, register: 4 */
    private Date deserializeToDate(JsonElement json) {
        Date parse;
        synchronized (this.localFormat) {
            try {
                parse = this.localFormat.parse(json.getAsString());
            } catch (ParseException e) {
                try {
                    return this.enUsFormat.parse(json.getAsString());
                } catch (ParseException e2) {
                    throw new JsonSyntaxException(json.getAsString(), e2);
                } catch (ParseException e3) {
                    return ISO8601Utils.parse(json.getAsString(), new ParsePosition(0));
                }
            }
        }
        return parse;
    }

    public String toString() {
        return DefaultDateTypeAdapter.class.getSimpleName() + '(' + this.localFormat.getClass().getSimpleName() + ')';
    }
}
