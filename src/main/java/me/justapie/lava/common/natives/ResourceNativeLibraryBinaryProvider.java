package me.justapie.lava.common.natives;

import org.slf4j.LoggerFactory;
import java.io.InputStream;
import me.justapie.lava.common.natives.architecture.SystemType;
import org.slf4j.Logger;

public class ResourceNativeLibraryBinaryProvider implements NativeLibraryBinaryProvider
{
    private static final Logger log;
    private final Class<?> classLoaderSample;
    private final String nativesRoot;
    
    public ResourceNativeLibraryBinaryProvider(final Class<?> classLoaderSample, final String nativesRoot) {
        this.classLoaderSample = ((classLoaderSample != null) ? classLoaderSample : ResourceNativeLibraryBinaryProvider.class);
        this.nativesRoot = nativesRoot;
    }
    
    @Override
    public InputStream getLibraryStream(final SystemType systemType, final String libraryName) {
        final String resourcePath = this.nativesRoot + systemType.formatSystemName() + "/" + systemType.formatLibraryName(libraryName);
        ResourceNativeLibraryBinaryProvider.log.debug("Native library {}: trying to find from resources at {} with {} as classloader reference", new Object[] { libraryName, resourcePath, this.classLoaderSample.getName() });
        return this.classLoaderSample.getResourceAsStream(resourcePath);
    }
    
    static {
        log = LoggerFactory.getLogger((Class)ResourceNativeLibraryBinaryProvider.class);
    }
}
