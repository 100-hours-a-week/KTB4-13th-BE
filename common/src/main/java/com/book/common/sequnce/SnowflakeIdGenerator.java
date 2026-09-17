package com.book.common.sequnce;

import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Enumeration;

public class SnowflakeIdGenerator implements IdGenerator {

    private static final int EPOCH_BITS = 41;
    private static final int NODE_ID_BITS = 10;
    private static final int SEQUENCE_BITS = 12;

    private static final long MAKE_NODE_ID = (1L << NODE_ID_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    // Custom Epoch (2020-01-01T00:00:00Z)
    private static final long DEFAULT_CUSTOM_EPOCH = 1_577_836_800_000L;

    private final long nodeId;
    private final long customEpoch;

    private volatile long lastTimestamp = -1L;
    private volatile long sequence = 0L;

    public SnowflakeIdGenerator() {
        this.nodeId = createNodeId();
        this.customEpoch = DEFAULT_CUSTOM_EPOCH;
    }

    @Override
    public synchronized Long nextId() {
        long currentTimestamp = timestamp();

        if (currentTimestamp < lastTimestamp) {
            throw new IllegalStateException(
                    "Invalid System Clock! current: (" + currentTimestamp
                            + ") last: (" + lastTimestamp + ")"
            );
        }

        if (currentTimestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;

            if (sequence == 0L) {
                currentTimestamp = waitNextMillis(currentTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return (currentTimestamp << (NODE_ID_BITS + SEQUENCE_BITS))
                | (nodeId << SEQUENCE_BITS)
                | sequence;
    }

    private long timestamp() {
        return Instant.now().toEpochMilli() - customEpoch;
    }

    private long waitNextMillis(long currentTimestamp) {
        long timestamp = currentTimestamp;

        while (timestamp == lastTimestamp) {
            timestamp = timestamp();
        }

        return timestamp;
    }

    private long createNodeId() {
        long nodeId;

        try {
            StringBuilder sb = new StringBuilder();

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    for (byte macPart : mac) {
                        sb.append(String.format("%02X", macPart));
                    }
                }
            }

            nodeId = sb.toString()
                    .hashCode();

        } catch (Exception exception) {
            nodeId = new SecureRandom()
                    .nextInt();
        }

        return nodeId & MAKE_NODE_ID;
    }

    @Override
    public String toString() {
        return "SnowflakeIdGenerator Settings [EPOCH_BITS="
                + EPOCH_BITS
                + ", NODE_ID_BITS="
                + NODE_ID_BITS
                + ", SEQUENCE_BITS="
                + SEQUENCE_BITS
                + ", CUSTOM_EPOCH="
                + customEpoch
                + ", NodeId="
                + nodeId
                + "]";
    }
}