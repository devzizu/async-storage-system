/*
 * Implements a simple interface for encoding/decoding
 * generic structures into a byte array. 
 * 
 * @author Grupo10-FSD
 * 
*/

package app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serialization {

    /**
     * Serializes an generic Object into a byte arrary.
     * 
     * @param data object to serialize
     * @return object in bytes
     * @throws IOException
     */
    public static byte[] serialize(Object data) throws IOException {

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(byteOut);
        out.writeObject(data);

        return byteOut.toByteArray();
    }

    /**
     * Deserializes a byte array into an generic Object.
     * 
     * @param bytes byte array to deserialize
     * @return Object deserialized
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {

        ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
        ObjectInputStream in = new ObjectInputStream(byteIn);

        Object result = in.readObject();

        return result;
    }
}
