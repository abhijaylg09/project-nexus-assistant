package com.nexus.core.events;

public class TelemetryUpdateEvent {
    private final double cpuUsagePercent;
    private final long memoryUsedMB;
    private final long memoryTotalMB;
    private final double visionFps;
    private final double audioLevel;
    private final boolean isListening;
    private final boolean isSpeaking;
    private final String activeMood;

    public TelemetryUpdateEvent(double cpuUsagePercent, long memoryUsedMB, long memoryTotalMB,
                                double visionFps, double audioLevel,
                                boolean isListening, boolean isSpeaking, String activeMood) {
        this.cpuUsagePercent = cpuUsagePercent;
        this.memoryUsedMB = memoryUsedMB;
        this.memoryTotalMB = memoryTotalMB;
        this.visionFps = visionFps;
        this.audioLevel = audioLevel;
        this.isListening = isListening;
        this.isSpeaking = isSpeaking;
        this.activeMood = activeMood;
    }

    public double getCpuUsagePercent() { return cpuUsagePercent; }
    public long getMemoryUsedMB() { return memoryUsedMB; }
    public long getMemoryTotalMB() { return memoryTotalMB; }
    public double getVisionFps() { return visionFps; }
    public double getAudioLevel() { return audioLevel; }
    public boolean isListening() { return isListening; }
    public boolean isSpeaking() { return isSpeaking; }
    public String getActiveMood() { return activeMood; }
}
