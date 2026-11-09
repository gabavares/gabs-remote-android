package com.limelight.gabs.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.limelight.nvstream.http.ComputerDetails;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PathSelectorTest {
    static Candidate c(PathKind kind, String ip) {
        return new Candidate(new ComputerDetails.AddressTuple(ip, 47989), kind);
    }

    static ParallelPathPoller.Result<String> r(PathKind kind, long rtt) {
        return new ParallelPathPoller.Result<>(c(kind, kind.name().toLowerCase()), rtt, "ok");
    }

    @SafeVarargs
    private static PathKind choose(ParallelPathPoller.Result<String>... rs) {
        return PathSelector.choose(new ArrayList<>(Arrays.asList(rs))).candidate.kind;
    }

    @Test
    public void lanAlwaysWins() {
        assertEquals(PathKind.LAN, choose(r(PathKind.LAN, 40), r(PathKind.IPV6, 5)));
    }

    @Test
    public void earlierKindWinsWithinTie() {
        assertEquals(PathKind.IPV6, choose(r(PathKind.TAILSCALE, 20), r(PathKind.IPV6, 28)));
        assertEquals(PathKind.TAILSCALE, choose(r(PathKind.TAILSCALE, 20), r(PathKind.IPV6, 31)));
        assertEquals(PathKind.IPV6, choose(r(PathKind.IPV4_PUBLICO, 25), r(PathKind.IPV6, 30)));
    }

    @Test
    public void emptyIsNull() {
        assertNull(PathSelector.choose(Collections.<ParallelPathPoller.Result<String>>emptyList()));
    }

    @Test
    public void filterKeepsOnlyForcedKind() {
        List<Candidate> all = Arrays.asList(c(PathKind.LAN, "192.168.15.2"), c(PathKind.TAILSCALE, "100.99.120.91"), c(PathKind.MANUAL, "pc.exemplo"));
        assertEquals(Collections.singletonList(all.get(1)), PathSelector.filter(all, PathKind.TAILSCALE));
        assertEquals(all, PathSelector.filter(all, null));
    }
}
