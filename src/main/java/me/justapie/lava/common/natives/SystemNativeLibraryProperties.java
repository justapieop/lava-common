package me.justapie.lava.common.natives;

public class SystemNativeLibraryProperties implements NativeLibraryProperties
{
    private final String libraryName;
    private final String propertyPrefix;
    
    public SystemNativeLibraryProperties(final String libraryName, final String propertyPrefix) {
        this.libraryName = libraryName;
        this.propertyPrefix = propertyPrefix;
    }
    
    @Override
    public String getLibraryPath() {
        return this.get("path");
    }
    
    @Override
    public String getLibraryDirectory() {
        return this.get("dir");
    }
    
    @Override
    public String getExtractionPath() {
        return this.get("extractPath");
    }
    
    @Override
    public String getSystemName() {
        return this.get("system");
    }
    
    @Override
    public String getArchitectureName() {
        return this.get("arch");
    }
    
    @Override
    public String getLibraryFileNamePrefix() {
        return this.get("libPrefix");
    }
    
    @Override
    public String getLibraryFileNameSuffix() {
        return this.get("libSuffix");
    }
    
    private String get(final String property) {
        return System.getProperty(this.propertyPrefix + this.libraryName + "." + property, System.getProperty(this.propertyPrefix + property));
    }
}
