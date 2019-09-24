package ninjaphenix.userdefinedadditions;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.impl.Marshaller;

public interface Serializer<T, R>
{
    T read(JsonObject object);

    JsonObject write(T object, Marshaller marshaller);

    R asRealObject();
}
