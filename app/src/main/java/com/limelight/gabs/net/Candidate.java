package com.limelight.gabs.net;

import com.limelight.nvstream.http.ComputerDetails;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Gabs Remote: um endereço do PC e o tipo de caminho dele. */
public final class Candidate {
    public final ComputerDetails.AddressTuple address;
    public final PathKind kind;

    public Candidate(ComputerDetails.AddressTuple address, PathKind kind) {
        this.address = address;
        this.kind = kind;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Candidate && ((Candidate) o).address.equals(address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        return kind + "|" + address.address + "|" + address.port;
    }

    /** Formato dos extras do Game: {@code kind|endereço|porta}. @return null se não seguir o formato */
    public static Candidate fromString(String s) {
        if (s == null) {
            return null;
        }
        int first = s.indexOf('|'), last = s.lastIndexOf('|');
        if (first <= 0 || last <= first) {
            return null;
        }
        try {
            return new Candidate(new ComputerDetails.AddressTuple(s.substring(first + 1, last),
                    Integer.parseInt(s.substring(last + 1))), PathKind.valueOf(s.substring(0, first)));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Os endereços conhecidos do PC, sem repetir, com o tipo de caminho de cada um (sem DNS). */
    public static List<Candidate> forComputer(ComputerDetails d) {
        List<Candidate> out = new ArrayList<>();
        HashSet<ComputerDetails.AddressTuple> seen = new HashSet<>();
        for (ComputerDetails.AddressTuple a : new ComputerDetails.AddressTuple[] {
                d.localAddress, d.manualAddress, d.remoteAddress, d.ipv6Address}) {
            if (a != null && seen.add(a)) {
                PathKind kind = PathKind.classifyAddress(a.address);
                if (a == d.manualAddress && kind != PathKind.TAILSCALE && kind != PathKind.LAN) {
                    kind = PathKind.MANUAL;
                }
                out.add(new Candidate(a, kind));
            }
        }
        return out;
    }
}
