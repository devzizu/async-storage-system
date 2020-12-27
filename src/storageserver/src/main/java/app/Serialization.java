package app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import app.data.ClientMessage;

public class Serialization {

    public static byte[] serialize(Object data) throws IOException {

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(data);

        return byteOut.toByteArray();
    }

    public static ClientMessage deserialize(byte[] bytes) throws IOException, ClassNotFoundException {

        ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(byteIn);

        Object result = in.readObject();

        return (ClientMessage) result;
    }
}
