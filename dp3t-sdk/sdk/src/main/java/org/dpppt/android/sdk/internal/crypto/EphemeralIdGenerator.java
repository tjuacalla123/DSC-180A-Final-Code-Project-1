package org.dpppt.android.sdk.internal.crypto;
import java.nio.ByteBuffer;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class EphemeralIdGenerator {

    private static final MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize SHA-256 hash function", e);
        }
    }

    private static final Map<Long, String> ID_TO_STRING_MAP = new HashMap<>();
    private static final Map<String, Long> STRING_TO_ID_MAP = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static byte[] generateEphemeralId(String input) {
        long id = generateRandomId();
        ID_TO_STRING_MAP.put(id, input);
        STRING_TO_ID_MAP.put(input, id);
        return ByteBuffer.allocate(8).putLong(id).array();
    }

    public static String reverseEphemeralId(byte[] idBytes) {
        long id = ByteBuffer.wrap(idBytes).getLong();
        return ID_TO_STRING_MAP.get(id);
    }

    private static long generateRandomId() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        byte[] hash = MESSAGE_DIGEST.digest(bytes);
        return ByteBuffer.wrap(hash).getLong();
    }
}

