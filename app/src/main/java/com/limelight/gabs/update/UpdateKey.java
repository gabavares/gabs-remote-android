package com.limelight.gabs.update;

/**
 * Chave pública Ed25519 que assina o manifesto da Release (decisão 0006). A privada fica só no PC
 * do Gab (docs/segredos.md). Base64 dos 32 bytes crus.
 */
public final class UpdateKey {
    public static final String PUBLIC_KEY_BASE64 = "vt/Fsd5CTf97gakAjSs4U3ugaDp5pM2hBkNKlA+RibA=";

    private UpdateKey() {}
}
