package KasperCommons.Parser;

import KasperCommons.Authenticator.KasperAccessAuthenticator;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.zip.DataFormatException;

public class DiskIO {
    private static String folder = "persistence/";
    private static String datapath = folder + "data.kasper";
    private static SecretKey secretKey;

    static {
        try {
            secretKey = EncryptionModule.generateKeyFromInt(KasperAccessAuthenticator.getHashedKey());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeDocument(KasperDocument document) throws Exception {
        Files.createDirectories(Path.of("persistence"));
        byte[] resolvedBytes = EncryptionModule.encrypt(document.toString(), secretKey);
        byte[] compressedBytes = ByteCompression.compress(resolvedBytes);
        try (DataOutputStream writer = new DataOutputStream(new FileOutputStream(datapath))) {
            writer.writeInt(compressedBytes.length);
            writer.write(compressedBytes);
        }
    }

    private static byte[] encryptedBuffer() throws IOException, DataFormatException {
        try (DataInputStream read = new DataInputStream(new FileInputStream(datapath))) {
            int size = read.readInt();
            byte[] buffer = new byte[size];
            read.readFully(buffer);
            return buffer;
        }
    }

    public static String documentStringRetrieval() throws Exception {
        byte[] compressedBytes = encryptedBuffer();
        byte[] resolvedBytes = ByteCompression.decompress(compressedBytes);
        return EncryptionModule.decrypt(resolvedBytes, secretKey);
    }

    public static class EncryptionModule {
        private static final String ALGORITHM = "AES";

        public static byte[] encrypt(String plaintext, SecretKey secretKey) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
            return cipher.doFinal(pad(plaintextBytes));
        }

        public static String decrypt(byte[] ciphertext, SecretKey secretKey) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(ciphertext);
            byte[] unpaddedBytes = unpad(decryptedBytes);
            return new String(unpaddedBytes, StandardCharsets.UTF_8);
        }

        private static byte[] pad(byte[] input) {
            int paddingLength = 16 - (input.length % 16);
            byte[] paddedInput = new byte[input.length + paddingLength];
            System.arraycopy(input, 0, paddedInput, 0, input.length);
            for (int i = input.length; i < paddedInput.length; i++) {
                paddedInput[i] = (byte) paddingLength;
            }
            return paddedInput;
        }

        private static byte[] unpad(byte[] input) {
            int paddingLength = input[input.length - 1];
            byte[] unpaddedInput = new byte[input.length - paddingLength];
            System.arraycopy(input, 0, unpaddedInput, 0, unpaddedInput.length);
            return unpaddedInput;
        }

        private static final String HASH_ALGORITHM = "SHA-256";

        private static SecretKey generateKeyFromInt(int value) throws Exception {
            // Convert the integer to a byte array
            ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES);
            byteBuffer.putInt(value);
            byte[] inputBytes = byteBuffer.array();

            // Apply HKDF to generate the secret key
            SecretKeySpec secretKeySpec = hkdfExtractAndExpand(inputBytes, null, 16);
            return secretKeySpec;
        }

        private static SecretKeySpec hkdfExtractAndExpand(byte[] inputKeyMaterial, byte[] salt, int keyLength) throws Exception {
            SecretKey secretKey = new SecretKeySpec(inputKeyMaterial, "HKDF");
            MessageDigest hashFunction = MessageDigest.getInstance(HASH_ALGORITHM);

            // HKDF Extract
            if (salt == null) {
                salt = new byte[hashFunction.getDigestLength()];
            }
            hashFunction.update(salt);
            byte[] pseudoRandomKey = hashFunction.digest(inputKeyMaterial);

            // HKDF Expand
            byte[] derivedKey = new byte[keyLength];
            System.arraycopy(pseudoRandomKey, 0, derivedKey, 0, keyLength);

            return new SecretKeySpec(derivedKey, "AES");
        }
    }
}