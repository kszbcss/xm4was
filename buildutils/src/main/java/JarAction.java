import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.internal.p2.core.helpers.FileUtils;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.IPublisherResult;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;

public class JarAction implements IPublisherAction {
    private final String classifier;
    private final String id;
    private final Version version;
    private final File file;

    public JarAction(String classifier, String id, Version version, File file) {
        this.classifier = classifier;
        this.id = id;
        this.version = version;
        this.file = file;
    }

    @Override
    public IStatus perform(IPublisherInfo info, IPublisherResult results, IProgressMonitor monitor) {
        IArtifactRepository repository = info.getArtifactRepository();
        IArtifactKey key = repository.createArtifactKey(classifier, id, version);
        IArtifactDescriptor descriptor = repository.createArtifactDescriptor(key);
        try {
            ;
            FileUtils.copyStream(new BufferedInputStream(new FileInputStream(file)), true, repository.getOutputStream(descriptor), true);
        } catch (ProvisionException ex) {
            return ex.getStatus();
        } catch (IOException ex) {
            return new Status(IStatus.ERROR, "importer", "Error publishing artifacts", ex);
        }
        return Status.OK_STATUS;
    }
}
