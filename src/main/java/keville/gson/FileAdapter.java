package keville.gson;

import java.io.File;
import java.lang.reflect.Type;

import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;


public final class FileAdapter implements JsonSerializer<File> {

  @Override
  public JsonElement serialize(File file, Type type, JsonSerializationContext jsonSerializationContext) {
    return new JsonPrimitive(file.getAbsolutePath());
  }

}


