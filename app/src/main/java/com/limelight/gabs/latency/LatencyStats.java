package com.limelight.gabs.latency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Gabs Remote: estatísticas de uma medição de latência (percentis pelo método do posto mais próximo). */
public class LatencyStats {
    private final List<Double> samples = new ArrayList<>();
    private final List<Integer> lostAt = new ArrayList<>();

    public void add(double ms) {
        samples.add(ms);
    }

    public void addLost() {
        lostAt.add(samples.size() + lostAt.size() + 1);
    }

    public int count() {
        return samples.size();
    }

    public int lost() {
        return lostAt.size();
    }

    public double avg() {
        if (samples.isEmpty()) {
            return Double.NaN;
        }
        double sum = 0;
        for (double s : samples) {
            sum += s;
        }
        return sum / samples.size();
    }

    public double p50() {
        return percentile(0.50);
    }

    public double p95() {
        return percentile(0.95);
    }

    public double max() {
        return samples.isEmpty() ? Double.NaN : Collections.max(samples);
    }

    private double percentile(double p) {
        if (samples.isEmpty()) {
            return Double.NaN;
        }
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);
        int rank = (int) Math.ceil(p * sorted.size());
        return sorted.get(Math.max(0, rank - 1));
    }

    /** CSV com uma linha por amostra; perdidos ficam com ms vazio. */
    public String toCsv(String mode) {
        StringBuilder sb = new StringBuilder("modo,amostra,ms\n");
        int n = 1, i = 0;
        for (int total = samples.size() + lostAt.size(); n <= total; n++) {
            if (lostAt.contains(n)) {
                sb.append(mode).append(',').append(n).append(",\n");
            } else {
                sb.append(mode).append(',').append(n).append(',')
                        .append(String.format(Locale.ROOT, "%.1f", samples.get(i++))).append('\n');
            }
        }
        return sb.toString();
    }
}
