package keville.gson;

import java.time.Instant;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonParseException;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Type;

public final class InstantAdapter implements JsonSerializer<Instant>,  JsonDeserializer<Instant> {

  @Override
  public Instant deserialize(JsonElement jsonElement , Type type, JsonDeserializationContext jsonDeserializationContext ) throws JsonParseException {
    String json  = jsonElement.getAsString();
    return Instant.parse(json); 
  }

  @Override
  public JsonElement serialize(Instant instant, Type type, JsonSerializationContext jsonSerializationContext) {
    return new JsonPrimitive(instant.toString());
  }

}


