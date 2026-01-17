package Arcadia.ClexaGod.arcadia.storage.pool;

public final class PostgresPoolAutoSizer {

    private PostgresPoolAutoSizer() {
    }

    public static PostgresPoolSizing calculate(PostgresPoolAutoConfig config) {
        if (config == null || !config.isEnabled()) {
            return new PostgresPoolSizing(false, 0, 0, 0, 0, 0, 0, 0);
        }

        int availableCores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int reservedCores = Math.max(0, config.getReserveCores());
        int usableCores = Math.max(1, availableCores - reservedCores);
        int targetFromCores = Math.max(1, usableCores * Math.max(1, config.getCoresMultiplier()));
        int targetFromPlayers = 0;
        if (config.getExpectedPlayers() > 0 && config.getPlayersPerConnection() > 0) {
            targetFromPlayers = (int) Math.ceil(config.getExpectedPlayers() / (double) config.getPlayersPerConnection());
        }
        int target = Math.max(targetFromCores, targetFromPlayers);
        int maxPoolSize = clamp(target, config.getMinSize(), config.getMaxSize());
        int minIdle = (int) Math.ceil(maxPoolSize * (config.getMinIdlePercent() / 100.0));
        minIdle = clamp(minIdle, 0, maxPoolSize);

        return new PostgresPoolSizing(
                true,
                availableCores,
                reservedCores,
                usableCores,
                targetFromCores,
                targetFromPlayers,
                maxPoolSize,
                minIdle
        );
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
