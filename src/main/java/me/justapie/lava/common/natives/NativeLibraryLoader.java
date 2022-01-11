package me.justapie.lava.common.natives;

import org.slf4j.LoggerFactory;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.FileSystems;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import me.justapie.lava.common.natives.architecture.SystemType;
import java.util.function.Predicate;
import org.slf4j.Logger;

public class NativeLibraryLoader
{
    private static final Logger log;
    private static final String DEFAULT_PROPERTY_PREFIX = "lava.native.";
    private static final String DEFAULT_RESOURCE_ROOT = "/natives/";
    private final String libraryName;
    private final Predicate<SystemType> systemFilter;
    private final NativeLibraryProperties properties;
    private final NativeLibraryBinaryProvider binaryProvider;
    private final Object lock;
    private volatile RuntimeException previousFailure;
    private volatile Boolean previousResult;
    
    public NativeLibraryLoader(final String libraryName, final Predicate<SystemType> systemFilter, final NativeLibraryProperties properties, final NativeLibraryBinaryProvider binaryProvider) {
        this.libraryName = libraryName;
        this.systemFilter = systemFilter;
        this.binaryProvider = binaryProvider;
        this.properties = properties;
        this.lock = new Object();
    }
    
    public static NativeLibraryLoader create(final Class<?> classLoaderSample, final String libraryName) {
        return createFiltered(classLoaderSample, libraryName, null);
    }
    
    public static NativeLibraryLoader createFiltered(final Class<?> classLoaderSample, final String libraryName, final Predicate<SystemType> systemFilter) {
        return new NativeLibraryLoader(libraryName, systemFilter, new SystemNativeLibraryProperties(libraryName, "lava.native."), new ResourceNativeLibraryBinaryProvider(classLoaderSample, "/natives/"));
    }
    
    public void load() {
        Boolean result = this.previousResult;
        if (result == null) {
            synchronized (this.lock) {
                result = this.previousResult;
                if (result == null) {
                    this.loadAndRemember();
                    return;
                }
            }
        }
        if (!result) {
            throw this.previousFailure;
        }
    }
    
    private void loadAndRemember() {
        NativeLibraryLoader.log.info("Native library {}: loading with filter {}", (Object)this.libraryName, (Object)this.systemFilter);
        try {
            this.loadInternal();
            this.previousResult = true;
        }
        catch (Throwable e) {
            NativeLibraryLoader.log.error("Native library {}: loading failed.", e);
            this.previousFailure = new RuntimeException(e);
            this.previousResult = false;
        }
    }
    
    private void loadInternal() {
        final String explicitPath = this.properties.getLibraryPath();
        if (explicitPath != null) {
            NativeLibraryLoader.log.debug("Native library {}: explicit path provided {}", (Object)this.libraryName, (Object)explicitPath);
            this.loadFromFile(Paths.get(explicitPath, new String[0]).toAbsolutePath());
        }
        else {
            final SystemType systemType = this.detectMatchingSystemType();
            if (systemType != null) {
                final String explicitDirectory = this.properties.getLibraryDirectory();
                if (explicitDirectory != null) {
                    NativeLibraryLoader.log.debug("Native library {}: explicit directory provided {}", (Object)this.libraryName, (Object)explicitDirectory);
                    this.loadFromFile(Paths.get(explicitDirectory, systemType.formatLibraryName(this.libraryName)).toAbsolutePath());
                }
                else {
                    this.loadFromFile(this.extractLibraryFromResources(systemType));
                }
            }
        }
    }
    
    private void loadFromFile(final Path libraryFilePath) {
        NativeLibraryLoader.log.debug("Native library {}: attempting to load library at {}", (Object)this.libraryName, (Object)libraryFilePath);
        System.load(libraryFilePath.toAbsolutePath().toString());
        NativeLibraryLoader.log.info("Native library {}: successfully loaded.", (Object)this.libraryName);
    }
    
    private Path extractLibraryFromResources(final SystemType systemType) {
        try (final InputStream libraryStream = this.binaryProvider.getLibraryStream(systemType, this.libraryName)) {
            if (libraryStream == null) {
                throw new UnsatisfiedLinkError("Required library was not found");
            }
            final Path extractedLibraryPath = this.prepareExtractionDirectory().resolve(systemType.formatLibraryName(this.libraryName));
            try (final FileOutputStream fileStream = new FileOutputStream(extractedLibraryPath.toFile())) {
                IOUtils.copy(libraryStream, (OutputStream)fileStream);
            }
            return extractedLibraryPath;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private Path prepareExtractionDirectory() throws IOException {
        final Path extractionDirectory = this.detectExtractionBaseDirectory().resolve(String.valueOf(System.currentTimeMillis()));
        if (!Files.isDirectory(extractionDirectory, new LinkOption[0])) {
            NativeLibraryLoader.log.debug("Native library {}: extraction directory {} does not exist, creating.", (Object)this.libraryName, (Object)extractionDirectory);
            try {
                createDirectoriesWithFullPermissions(extractionDirectory);
                return extractionDirectory;
            }
            catch (FileAlreadyExistsException ex) {
                return extractionDirectory;
            }
            catch (IOException e) {
                throw new IOException("Failed to create directory for unpacked native library.", e);
            }
        }
        NativeLibraryLoader.log.debug("Native library {}: extraction directory {} already exists, using.", (Object)this.libraryName, (Object)extractionDirectory);
        return extractionDirectory;
    }
    
    private Path detectExtractionBaseDirectory() {
        final String explicitExtractionBase = this.properties.getExtractionPath();
        if (explicitExtractionBase != null) {
            NativeLibraryLoader.log.debug("Native library {}: explicit extraction path provided - {}", (Object)this.libraryName, (Object)explicitExtractionBase);
            return Paths.get(explicitExtractionBase, new String[0]).toAbsolutePath();
        }
        final Path path = Paths.get(System.getProperty("java.io.tmpdir", "/tmp"), "lava-jni-natives").toAbsolutePath();
        NativeLibraryLoader.log.debug("Native library {}: detected {} as base directory for extraction.", (Object)this.libraryName, (Object)path);
        return path;
    }
    
    private SystemType detectMatchingSystemType() {
        SystemType systemType;
        try {
            systemType = SystemType.detect(this.properties);
        }
        catch (IllegalArgumentException e) {
            if (this.systemFilter != null) {
                NativeLibraryLoader.log.info("Native library {}: could not detect sytem type, but system filter is {} - assuming it does not match and skipping library.", (Object)this.libraryName, (Object)this.systemFilter);
                return null;
            }
            throw e;
        }
        if (this.systemFilter != null && !this.systemFilter.test(systemType)) {
            NativeLibraryLoader.log.debug("Native library {}: system filter does not match detected system {], skipping", (Object)this.libraryName, (Object)systemType.formatSystemName());
            return null;
        }
        return systemType;
    }
    
    private static void createDirectoriesWithFullPermissions(final Path path) throws IOException {
        final boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        if (!isPosix) {
            Files.createDirectories(path, (FileAttribute<?>[])new FileAttribute[0]);
        }
        else {
            Files.createDirectories(path, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));
        }
    }
    
    static {
        log = LoggerFactory.getLogger((Class)NativeLibraryLoader.class);
    }
}
