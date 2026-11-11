package com.limelight.gabs.mic;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.crypto.SecretKey;

/**
 * Gabs Remote: manda os quadros Opus do microfone para a UDP 48002 do PC.
 * O seq vive aqui, na conexão: mudo e volta continuam a contagem (nunca repete IV com a mesma chave).
 */
public final class MicSender implements MicController.Sender {
    public static final int PORT = 48002;

    /** Para onde vão os pacotes (UDP de verdade no app; lista nos testes). */
    public interface Transport {
        void send(byte[] packet) throws IOException;
    }

    private final Transport transport;
    private volatile SecretKey key;
    private int seq;

    public MicSender(Transport transport, SecretKey key) {
        this.transport = transport;
        this.key = key;
    }

    @Override
    public void resetKey(SecretKey newKey) {
        key = newKey;
    }

    public synchronized void send(byte[] opus, int len) throws IOException {
        transport.send(MicPacket.seal(seq++, opus, len, key));
    }

    /** Transporte UDP para o host da sessão (resolve o nome na primeira vez, fora da thread da interface). */
    public static Transport udp(final String host) {
        return new Transport() {
            private DatagramSocket socket;
            private InetAddress address;

            @Override
            public void send(byte[] packet) throws IOException {
                if (socket == null) {
                    address = InetAddress.getByName(host);
                    socket = new DatagramSocket();
                }
                socket.send(new DatagramPacket(packet, packet.length, address, PORT));
            }
        };
    }
}
