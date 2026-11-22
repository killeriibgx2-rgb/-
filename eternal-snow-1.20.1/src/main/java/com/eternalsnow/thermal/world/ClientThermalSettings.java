package com.eternalsnow.thermal.world;

/** Client-side copy of the server-authoritative settings shown in the admin screen. */
public final class ClientThermalSettings {
    private static Snapshot snapshot;
    private static int revision;

    private ClientThermalSettings() {
    }

    public static void clear() {
        snapshot = null;
        revision++;
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    public static void update(boolean allowed, boolean applied, boolean enabled,
                              double ambientCelsius, double speedMultiplier, int zoneRadius,
                              boolean bodyEnabled, boolean doorInfiltration) {
        snapshot = new Snapshot(++revision, allowed, applied, enabled, ambientCelsius,
                speedMultiplier, zoneRadius, bodyEnabled, doorInfiltration);
    }

    public static final class Snapshot {
        private final int revision;
        private final boolean allowed;
        private final boolean applied;
        private final boolean enabled;
        private final double ambientCelsius;
        private final double speedMultiplier;
        private final int zoneRadius;
        private final boolean bodyEnabled;
        private final boolean doorInfiltration;

        private Snapshot(int revision, boolean allowed, boolean applied, boolean enabled,
                         double ambientCelsius, double speedMultiplier, int zoneRadius,
                         boolean bodyEnabled, boolean doorInfiltration) {
            this.revision = revision;
            this.allowed = allowed;
            this.applied = applied;
            this.enabled = enabled;
            this.ambientCelsius = ambientCelsius;
            this.speedMultiplier = speedMultiplier;
            this.zoneRadius = zoneRadius;
            this.bodyEnabled = bodyEnabled;
            this.doorInfiltration = doorInfiltration;
        }

        public int revision() { return revision; }
        public boolean allowed() { return allowed; }
        public boolean applied() { return applied; }
        public boolean enabled() { return enabled; }
        public double ambientCelsius() { return ambientCelsius; }
        public double speedMultiplier() { return speedMultiplier; }
        public int zoneRadius() { return zoneRadius; }
        public boolean bodyEnabled() { return bodyEnabled; }
        public boolean doorInfiltration() { return doorInfiltration; }
    }
}
