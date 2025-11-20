package com.kakaotechbootcamp.community.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

public class SerializationUtil {

    public static String serialize(Object object) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(object);
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("객체 직렬화 실패", e);
        }
    }

    public static <T> T deserialize(String value, Class<T> cls) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getUrlDecoder().decode(value));
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return cls.cast(ois.readObject());
        } catch (Exception e) {
            throw new RuntimeException("객체 역직렬화 실패", e);
        }
    }
}
