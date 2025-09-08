package dev.paraplan.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "advisor")
public class AdvisorProperties {

  public static class Thresholds {
    private long seqscanRowcount = 50_000;
    private int tempSpillRisk = 2;
    private int ioRisk = 2;

    public long getSeqscanRowcount() { return seqscanRowcount; }
    public void setSeqscanRowcount(long v) { this.seqscanRowcount = v; }
    public int getTempSpillRisk() { return tempSpillRisk; }
    public void setTempSpillRisk(int v) { this.tempSpillRisk = v; }
    public int getIoRisk() { return ioRisk; }
    public void setIoRisk(int v) { this.ioRisk = v; }
  }

  public static class Defaults {
    private boolean enableLandscape = true;
    private boolean enableDcc = true;
    private int mcSamples = 25;

    public boolean isEnableLandscape() { return enableLandscape; }
    public void setEnableLandscape(boolean v) { this.enableLandscape = v; }
    public boolean isEnableDcc() { return enableDcc; }
    public void setEnableDcc(boolean v) { this.enableDcc = v; }
    public int getMcSamples() { return mcSamples; }
    public void setMcSamples(int v) { this.mcSamples = v; }
  }

  private Thresholds thresholds = new Thresholds();
  private Defaults defaults = new Defaults();

  public Thresholds getThresholds() { return thresholds; }
  public void setThresholds(Thresholds t) { this.thresholds = t; }
  public Defaults getDefaults() { return defaults; }
  public void setDefaults(Defaults d) { this.defaults = d; }
}
