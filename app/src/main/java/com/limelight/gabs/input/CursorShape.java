package com.limelight.gabs.input;

/**
 * Gabs Remote: formato do cursor do Windows, como o host conta no /gabs/cursor (grCursor=local).
 * OTHER = cursor próprio de um programa (o app desenha a seta).
 */
public enum CursorShape {
    HIDDEN, ARROW, IBEAM, HAND, WAIT, APP_STARTING, CROSS,
    SIZE_NS, SIZE_WE, SIZE_NWSE, SIZE_NESW, SIZE_ALL, NO, HELP, OTHER;

    /** Resposta do /gabs/cursor: "versão formato". */
    public static final class Reply {
        public final long version;
        public final CursorShape shape;

        Reply(long version, CursorShape shape) {
            this.version = version;
            this.shape = shape;
        }
    }

    /** Nome do host (ex.: "size_nwse") → formato; nome desconhecido = OTHER. */
    public static CursorShape fromHostName(String name) {
        for (CursorShape s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return OTHER;
    }

    /** @return a resposta, ou null se o texto não for "versão formato". */
    public static Reply parseReply(String body) {
        if (body == null) {
            return null;
        }
        String[] parts = body.trim().split("\\s+");
        if (parts.length != 2) {
            return null;
        }
        try {
            return new Reply(Long.parseLong(parts[0]), fromHostName(parts[1]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
