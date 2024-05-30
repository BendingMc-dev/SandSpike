package org.bmc;

public enum Config {
    RANGE("ExtraAbilities.Bera.SandSpike.Range"),
    MAX_HEIGHT("ExtraAbilities.Bera.SandSpike.MaxHeight"),
    COOLDOWN("ExtraAbilities.Bera.SandSpike.Cooldown"),
    SOURCE_RANGE("ExtraAbilities.Bera.SandSpike.SourceRange");

    final String path;

    Config(String path) {
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}
