import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.publisher.IPublisherAction;
import org.eclipse.equinox.p2.publisher.IPublisherInfo;
import org.eclipse.equinox.p2.publisher.Publisher;
import org.eclipse.equinox.p2.publisher.PublisherInfo;
import org.eclipse.equinox.p2.publisher.eclipse.BundlesAction;
import org.eclipse.equinox.p2.repository.artifact.ArtifactKeyQuery;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepository;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.veithen.cosmos.osgi.runtime.Configuration;
import com.github.veithen.cosmos.osgi.runtime.CosmosException;
import com.github.veithen.cosmos.osgi.runtime.Runtime;
import com.github.veithen.cosmos.osgi.runtime.RuntimeInitializer;
import com.github.veithen.cosmos.osgi.runtime.equinox.EquinoxInitializer;
import com.github.veithen.cosmos.osgi.runtime.logging.simple.SimpleLogger;
import com.google.common.io.Files;

public class Importer {
    private static final Set<String> ignore = new HashSet<String>(Arrays.asList("META-INF/ECLIPSEF.RSA", "META-INF/ECLIPSEF.SF"));
    // These bundles are required to run the unit tests
    private static String[] eclipsePlugins = { "org.eclipse.equinox.launcher", "org.junit", "org.hamcrest.core" };
    
    public static void main(String[] args) throws Exception {
        final File outputDir = Files.createTempDir();
        final File p2DataArea = Files.createTempDir();
        java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(outputDir);
                    FileUtils.deleteDirectory(p2DataArea);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        URI repoURI = new URI(args[args.length-1]);
        Runtime runtime = Runtime.getInstance(Configuration.newDefault().logger(SimpleLogger.INSTANCE).initializer(new RuntimeInitializer() {
            @Override
            public void initializeRuntime(Runtime runtime) throws CosmosException, BundleException {
                EquinoxInitializer.INSTANCE.initializeRuntime(runtime);
                runtime.setProperty("eclipse.p2.data.area", p2DataArea.getAbsolutePath());
                // Don't use mirrors because they make the execution more unpredictable
                runtime.setProperty("eclipse.p2.mirrors", "false");
                runtime.getBundle("org.apache.felix.scr").start();
                runtime.getBundle("org.eclipse.equinox.p2.core").start();
            }
        }).build());
        IProgressMonitor monitor = new SystemOutProgressMonitor();
        IProvisioningAgent agent = runtime.getService(IProvisioningAgent.class);
        IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
        IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);

        // Create repository early so that we can fail early
        IArtifactRepository artifactRepository = artifactRepositoryManager.createRepository(repoURI, "WebSphere Artifact Repository", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, Collections.<String,String>emptyMap());
        IMetadataRepository metadataRepository = metadataRepositoryManager.createRepository(repoURI, "WebSphere Metadata Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, Collections.<String,String>emptyMap());
        
        List<IPublisherAction> publisherActions = new ArrayList<IPublisherAction>();
        for (int i=0; i<args.length-1; i++) {
            publisherActions.add(new BundlesAction(processWASPlugins(new File(args[i]), outputDir)));
        }
        
        publisherActions.add(new BundlesAction(downloadEclipsePlugins(artifactRepositoryManager, outputDir, monitor)));
        
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactRepository(artifactRepository);
        publisherInfo.setMetadataRepository(metadataRepository);
        publisherInfo.setArtifactOptions(IPublisherInfo.A_PUBLISH | IPublisherInfo.A_INDEX);
        Publisher publisher = new Publisher(publisherInfo);
        IStatus status = publisher.publish(publisherActions.toArray(new IPublisherAction[publisherActions.size()]), monitor);
        // TODO: need a shutdown method for the OSGi runtime (to stop non daemon threads)
        if (status.isOK()) {
            System.exit(0);
        } else {
            System.err.println("STATUS: " + status);
            System.exit(1);
        }
    }
    
    private static File[] processWASPlugins(final File wasDir, File outputDir) throws Exception {
        List<File> outputFiles = new ArrayList<File>();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(false);
        dbf.setValidating(false);
        DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
        documentBuilder.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (systemId != null && systemId.endsWith("/product.dtd")) {
                    return new InputSource(new File(wasDir, "properties/version/dtd/product.dtd").toURI().toString());
                } else {
                    return null;
                }
            }
        });
        Document document = documentBuilder.parse(new File(wasDir, "properties/version/WAS.product"));
        final String wasVersion = ((Element)document.getElementsByTagName("version").item(0)).getTextContent();
        System.out.println("WAS version is " + wasVersion);
        final String bundleVersionSuffix = "WAS_" + wasVersion.replace(".", "_");
        for (File plugin : new File(wasDir, "plugins").listFiles()) {
            String name = plugin.getName();
            if (plugin.isFile() && name.endsWith(".jar")) {
                System.out.println(plugin);
                File outputFile = new File(outputDir, name.substring(0, name.length()-4) + "_" + wasVersion + ".jar");
                transformJAR(plugin, outputFile, new ManifestTransformer() {
                    @Override
                    public void transformManifest(Manifest manifest) {
                        Attributes atts = manifest.getMainAttributes();
                        String bundleVersion = atts.getValue("Bundle-Version");
                        int dots = 0;
                        for (int i=0; i<bundleVersion.length(); i++) {
                            if (bundleVersion.charAt(i) == '.') {
                                dots++;
                            }
                        }
                        atts.putValue("Bundle-Version", bundleVersion + (dots == 3 ? "_" : (dots == 2 ? "." : ".0.")) + bundleVersionSuffix);
                        // Remove signatures
                        manifest.getEntries().clear();
                    }
                });
                outputFiles.add(outputFile);
            }
        }
        // Hack: bootstrap.jar is configured as visible to all bundles (using org.osgi.framework.bootdelegation);
        // Instead we load it as a fragment into com.ibm.ws.bootstrap and com.ibm.ws.runtime.
        File outputFile = new File(outputDir, "bootstrap-" + wasVersion + ".jar");
        transformJAR(new File(wasDir, "lib/bootstrap.jar"), outputFile, new ManifestTransformer() {
            @Override
            public void transformManifest(Manifest manifest) {
                Attributes atts = manifest.getMainAttributes();
                atts.putValue("Bundle-ManifestVersion", "2");
                atts.putValue("Bundle-SymbolicName", "bootstrap");
                atts.putValue("Bundle-Version", wasVersion);
                atts.putValue("Fragment-Host", "com.ibm.ws.bootstrap");
            }
        });
        outputFiles.add(outputDir);
        outputFile = new File(outputDir, "bootstrap-runtime-" + wasVersion + ".jar");
        transformJAR(new File(wasDir, "lib/bootstrap.jar"), outputFile, new ManifestTransformer() {
            @Override
            public void transformManifest(Manifest manifest) {
                Attributes atts = manifest.getMainAttributes();
                atts.putValue("Bundle-ManifestVersion", "2");
                atts.putValue("Bundle-SymbolicName", "bootstrap-runtime");
                atts.putValue("Bundle-Version", wasVersion);
                atts.putValue("Fragment-Host", "com.ibm.ws.runtime");
            }
        });
        outputFiles.add(outputDir);
        return outputFiles.toArray(new File[outputFiles.size()]);
    }
    
    private static void transformJAR(File inputFile, File outputFile, ManifestTransformer manifestTransformer) throws Exception {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(inputFile));
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile));
        byte[] buffer = new byte[4096];
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            String entryName = entry.getName();
            if (ignore.contains(entryName)) {
                continue;
            }
            zout.putNextEntry(new ZipEntry(entryName));
            if (entryName.equals("META-INF/MANIFEST.MF")) {
                Manifest manifest = new Manifest(zin);
                manifestTransformer.transformManifest(manifest);
                manifest.write(zout);
            } else {
                int c;
                while ((c = zin.read(buffer)) != -1) {
                    zout.write(buffer, 0, c);
                }
            }
        }
        zin.close();
        zout.close();
    }
    
    private static File[] downloadEclipsePlugins(IArtifactRepositoryManager artifactRepositoryManager, File outputDir, IProgressMonitor monitor) throws Exception {
        IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository(new URI("http://download.eclipse.org/releases/kepler/201306260900"), monitor);
        List<File> result = new ArrayList<File>();
        for (String id : eclipsePlugins) {
            for (IArtifactKey key : artifactRepository.query(new ArtifactKeyQuery("osgi.bundle", id, null), monitor)) {
                IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
                File outputFile = new File(outputDir, id + "_" + key.getVersion() + ".jar");
                FileOutputStream out = new FileOutputStream(outputFile);
                try {
                    artifactRepository.getArtifact(descriptors[0], out, monitor);
                } finally {
                    out.close();
                }
                result.add(outputFile);
            }
        }
        return result.toArray(new File[result.size()]);
    }
}
