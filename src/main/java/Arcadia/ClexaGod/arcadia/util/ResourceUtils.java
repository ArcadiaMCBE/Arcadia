package Arcadia.ClexaGod.arcadia.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@UtilityClass
public class ResourceUtils {

    public static void copyResource(ClassLoader classLoader, String resourceName, Path targetPath) throws IOException {
        try (InputStream input = classLoader.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing resource: " + resourceName);
            }
            Path parent = targetPath.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
