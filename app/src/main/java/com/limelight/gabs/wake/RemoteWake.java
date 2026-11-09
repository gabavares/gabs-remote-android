package com.limelight.gabs.wake;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.limelight.LimeLog;
import com.limelight.gabs.net.GabsNetPrefs;
import com.limelight.gabs.net.PathKind;
import com.limelight.gabs.net.PresenceClient;
import com.limelight.nvstream.http.ComputerDetails;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Gabs Remote: liga o PC de fora de casa (decisão 0005). O app manda o pacote mágico por UDP para
 * o IPv4 público de casa, na porta que o roteador redireciona ao PC (UDP 8080, a regra da antiga
 * WOLskill). O IPv4 vem da página de presença, do último valor conhecido ou do endereço remoto
 * que o upstream descobre por STUN em casa.
 */
public final class RemoteWake {
    public enum Method { NENHUM, ROTEADOR }

    public static final String PREF_METHOD = "gr_wake_method";
    public static final String PREF_PORT = "gr_wake_port";
    public static final int DEFAULT_PORT = 8080;
    private static final String PREF_LAST_HOME = "gr_home_ipv4_";

    private RemoteWake() {
    }

    public static Method parse(String pref) {
        if (pref == null) {
            return Method.NENHUM;
        }
        try {
            return Method.valueOf(pref);
        } catch (IllegalArgumentException e) {
            return Method.NENHUM;
        }
    }

    public static int parsePort(String pref) {
        try {
            int p = Integer.parseInt(pref == null ? "" : pref.trim());
            return p >= 1 && p <= 65535 ? p : DEFAULT_PORT;
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    /** @return o primeiro IPv4 público entre os dois, ou null */
    public static String homeIpv4(String fromPresence, String lastKnown) {
        for (String ip : new String[] {fromPresence, lastKnown}) {
            if (ip != null && PathKind.classifyAddress(ip) == PathKind.IPV4_PUBLICO) {
                return ip;
            }
        }
        return null;
    }

    public static Method method(Context c) {
        return parse(PreferenceManager.getDefaultSharedPreferences(c).getString(PREF_METHOD, Method.ROTEADOR.name()));
    }

    /**
     * Manda o pacote pelo roteador. Bloqueia (rede): chamar fora da thread da interface.
     * @return true se o pacote saiu
     */
    public static boolean trigger(Context c, ComputerDetails pc) {
        if (method(c) != Method.ROTEADOR) {
            return false;
        }
        byte[] packet = MagicPacket.build(pc.macAddress);
        if (packet == null) {
            LimeLog.warning("Gabs Remote: sem MAC para o WoL de fora");
            return false;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
        PresenceClient.Info info = PresenceClient.fetch(GabsNetPrefs.presenceUrl(c), GabsNetPrefs.presenceToken(c), pc.uuid);
        String last = prefs.getString(PREF_LAST_HOME + pc.uuid, null);
        if (last == null && pc.remoteAddress != null) {
            last = pc.remoteAddress.address;
        }
        String home = homeIpv4(info != null ? info.ipv4 : null, last);
        if (home == null) {
            LimeLog.warning("Gabs Remote: IP de casa desconhecido para o WoL de fora");
            return false;
        }
        prefs.edit().putString(PREF_LAST_HOME + pc.uuid, home).apply();

        int port = parsePort(prefs.getString(PREF_PORT, String.valueOf(DEFAULT_PORT)));
        try (DatagramSocket sock = new DatagramSocket()) {
            InetAddress target = InetAddress.getByName(home);
            for (int i = 0; i < 3; i++) {
                sock.send(new DatagramPacket(packet, packet.length, target, port));
            }
            LimeLog.info("Gabs Remote: pacote mágico enviado ao roteador de casa, porta " + port);
            return true;
        } catch (Exception e) {
            LimeLog.warning("Gabs Remote: falha ao mandar o pacote mágico: " + e);
            return false;
        }
    }
}
