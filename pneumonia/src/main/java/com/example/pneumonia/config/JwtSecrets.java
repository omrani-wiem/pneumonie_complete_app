package com.example.pneumonia.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public final  class JwtSecrets {

    private static final int MIN_BYTES = 32;
     private JwtSecrets() {}


     public static SecretKey key(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET n'est pas défini. Définissez la variable d'environnement JWT_SECRET "
                            + "(32 caractères minimum, ex. : openssl rand -base64 48) "
                            + "ou lancez avec le profil 'dev' en local.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court (" + bytes.length + " octets, minimum " + MIN_BYTES + ").");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }
}
