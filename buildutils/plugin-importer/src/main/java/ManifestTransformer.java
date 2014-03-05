import java.util.jar.Manifest;

public interface ManifestTransformer {
    boolean transformManifest(Manifest manifest);
}
