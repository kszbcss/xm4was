import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
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
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactRepository;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.metadata.IArtifactKey;
import org.eclipse.equinox.p2.metadata.Version;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.github.veithen.cosmos.osgi.runtime.CosmosRuntime;
import com.google.common.io.Files;

public class Importer {
    private static final Set<String> ignore = new HashSet<String>(Arrays.asList("META-INF/ECLIPSEF.RSA", "META-INF/ECLIPSEF.SF"));
    // These bundles are required to run the unit tests
    private static String[] eclipsePlugins = { "org.eclipse.equinox.launcher", "org.junit", "org.hamcrest.core" };
    
    private static final String[][] mappingRules = {
        { "(&(classifier=osgi.bundle))", "${repoUrl}/plugins/${id}_${version}.jar" },
        { "(&(classifier=websphere-library))", "${repoUrl}/lib/${id}_${version}.jar" },
    };
    
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
        IProgressMonitor monitor = new SystemOutProgressMonitor();
        IProvisioningAgent agent = CosmosRuntime.getInstance().getService(IProvisioningAgentProvider.class).createAgent(p2DataArea.toURI());
        IArtifactRepositoryManager artifactRepositoryManager = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);
        IMetadataRepositoryManager metadataRepositoryManager = (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);

        // Create repository early so that we can fail early
        IArtifactRepository artifactRepository = artifactRepositoryManager.createRepository(repoURI, "WebSphere Artifact Repository", IArtifactRepositoryManager.TYPE_SIMPLE_REPOSITORY, Collections.<String,String>emptyMap());
        setRules(artifactRepository, mappingRules);
        IMetadataRepository metadataRepository = metadataRepositoryManager.createRepository(repoURI, "WebSphere Metadata Repository", IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, Collections.<String,String>emptyMap());
        
        List<IPublisherAction> actions = new ArrayList<IPublisherAction>();
        actions.add(new BundlesAction(new File[] { outputDir }));
        
        for (int i=0; i<args.length-1; i++) {
            File wasDir = new File(args[i]);
            String wasVersion = processWASPlugins(wasDir, outputDir);
            actions.add(new JarAction("websphere-library", "bootstrap", Version.create(wasVersion), new File(wasDir, "lib/bootstrap.jar")));
        }
        
        downloadEclipsePlugins(artifactRepositoryManager, outputDir, monitor);
        
        PublisherInfo publisherInfo = new PublisherInfo();
        publisherInfo.setArtifactRepository(artifactRepository);
        publisherInfo.setMetadataRepository(metadataRepository);
        publisherInfo.setArtifactOptions(IPublisherInfo.A_PUBLISH | IPublisherInfo.A_INDEX);
        Publisher publisher = new Publisher(publisherInfo);
        IStatus status = publisher.publish(actions.toArray(new IPublisherAction[actions.size()]), monitor);
        // TODO: need a shutdown method for the OSGi runtime (to stop non daemon threads)
        if (status.isOK()) {
            System.exit(0);
        } else {
            System.err.println("STATUS: " + status);
            System.exit(1);
        }
    }
    
    private static void setRules(IArtifactRepository artifactRepository, String[][] mappingRules) throws Exception {
        SimpleArtifactRepository simpleArtifactRepository = (SimpleArtifactRepository)artifactRepository;
        simpleArtifactRepository.setRules(mappingRules);
        Method initializeMapper = SimpleArtifactRepository.class.getDeclaredMethod("initializeMapper");
        initializeMapper.setAccessible(true);
        initializeMapper.invoke(simpleArtifactRepository);
    }
    
    private static String processWASPlugins(final File wasDir, File outputDir) throws Exception {
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
                boolean result = transformJAR(plugin, new File(outputDir, name.substring(0, name.length()-4) + "_" + wasVersion + ".jar"), new ManifestTransformer() {
                    @Override
                    public boolean transformManifest(Manifest manifest) {
                        Attributes atts = manifest.getMainAttributes();
                        String bundleVersion = atts.getValue("Bundle-Version");
                        if (bundleVersion == null) {
                            // The plugins folder of WPS 6.1 contains JARs that are not bundles.
                            return false;
                        } else {
                            int dots = 0;
                            for (int i=0; i<bundleVersion.length(); i++) {
                                if (bundleVersion.charAt(i) == '.') {
                                    dots++;
                                }
                            }
                            atts.putValue("Bundle-Version", bundleVersion + (dots == 3 ? "_" : (dots == 2 ? "." : ".0.")) + bundleVersionSuffix);
                            // Remove signatures
                            manifest.getEntries().clear();
                            return true;
                        }
                    }
                });
                if (!result) {
                    System.out.println("  Skipped. Not a bundle.");
                }
            }
        }
        return wasVersion;
    }
    
    private static boolean transformJAR(File inputFile, File outputFile, ManifestTransformer manifestTransformer) throws Exception {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(inputFile));
        ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(outputFile));
        boolean result = false;
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
                result = manifestTransformer.transformManifest(manifest);
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
        if (!result) {
            outputFile.delete();
        }
        return result;
    }
    
    private static void downloadEclipsePlugins(IArtifactRepositoryManager artifactRepositoryManager, File outputDir, IProgressMonitor monitor) throws Exception {
        IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository(new URI("http://download.eclipse.org/releases/kepler/201306260900"), monitor);
        for (String id : eclipsePlugins) {
            for (IArtifactKey key : artifactRepository.query(new ArtifactKeyQuery("osgi.bundle", id, null), monitor)) {
                IArtifactDescriptor[] descriptors = artifactRepository.getArtifactDescriptors(key);
                FileOutputStream out = new FileOutputStream(new File(outputDir, id + "_" + key.getVersion() + ".jar"));
                try {
                    artifactRepository.getArtifact(descriptors[0], out, monitor);
                } finally {
                    out.close();
                }
            }
        }
    }
}
