package me.justapie.lava.common.natives.architecture;

public enum DefaultOperatingSystemTypes implements OperatingSystemType
{
    LINUX("linux", "lib", ".so"), 
    WINDOWS("win", "", ".dll"), 
    DARWIN("darwin", "lib", ".dylib"), 
    SOLARIS("solaris", "lib", ".so");
    
    private final String identifier;
    private final String libraryFilePrefix;
    private final String libraryFileSuffix;
    
    private DefaultOperatingSystemTypes(final String identifier, final String libraryFilePrefix, final String libraryFileSuffix) {
        this.identifier = identifier;
        this.libraryFilePrefix = libraryFilePrefix;
        this.libraryFileSuffix = libraryFileSuffix;
    }
    
    @Override
    public String identifier() {
        return this.identifier;
    }
    
    @Override
    public String libraryFilePrefix() {
        return this.libraryFilePrefix;
    }
    
    @Override
    public String libraryFileSuffix() {
        return this.libraryFileSuffix;
    }
    
    public static OperatingSystemType detect() {
        final String osFullName = System.getProperty("os.name");
        if (osFullName.startsWith("Windows")) {
            return DefaultOperatingSystemTypes.WINDOWS;
        }
        if (osFullName.startsWith("Mac OS X")) {
            return DefaultOperatingSystemTypes.DARWIN;
        }
        if (osFullName.startsWith("Solaris")) {
            return DefaultOperatingSystemTypes.SOLARIS;
        }
        if (osFullName.toLowerCase().startsWith("linux")) {
            return DefaultOperatingSystemTypes.LINUX;
        }
        throw new IllegalArgumentException("Unknown operating system: " + osFullName);
    }
}
