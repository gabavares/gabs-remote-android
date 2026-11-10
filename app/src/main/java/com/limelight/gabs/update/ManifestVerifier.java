package com.limelight.gabs.update;

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;

/** Confere a assinatura Ed25519 do manifesto (decisão 0006). */
public final class ManifestVerifier {
    private ManifestVerifier() {}

    /** @param publicKeyRaw os 32 bytes crus da chave pública; signature com 64 bytes. */
    public static boolean verify(byte[] data, byte[] signature, byte[] publicKeyRaw) {
        if (data == null || signature == null || publicKeyRaw == null
                || signature.length != 64 || publicKeyRaw.length != 32) {
            return false;
        }
        try {
            Ed25519Signer signer = new Ed25519Signer();
            signer.init(false, new Ed25519PublicKeyParameters(publicKeyRaw, 0));
            signer.update(data, 0, data.length);
            return signer.verifySignature(signature);
        } catch (RuntimeException e) {
            return false;
        }
    }
}
