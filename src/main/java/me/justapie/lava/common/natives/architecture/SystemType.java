package me.justapie.lava.common.natives.architecture;

import java.util.Optional;
import me.justapie.lava.common.natives.NativeLibraryProperties;

public class SystemType
{
    public final ArchitectureType architectureType;
    public final OperatingSystemType osType;
    
    public SystemType(final ArchitectureType architectureType, final OperatingSystemType osType) {
        this.architectureType = architectureType;
        this.osType = osType;
    }
    
    public String formatSystemName() {
        if (this.osType.identifier() == null) {
            return this.architectureType.identifier();
        }
        if (this.osType == DefaultOperatingSystemTypes.DARWIN) {
            return this.osType.identifier();
        }
        return this.osType.identifier() + "-" + this.architectureType.identifier();
    }
    
    public String formatLibraryName(final String libraryName) {
        return this.osType.libraryFilePrefix() + libraryName + this.osType.libraryFileSuffix();
    }
    
    public static SystemType detect(final NativeLibraryProperties properties) {
        final String systemName = properties.getSystemName();
        if (systemName != null) {
            return new SystemType(() -> systemName, new UnknownOperatingSystem((String)Optional.ofNullable(properties.getLibraryFileNamePrefix()).orElse("lib"), (String)Optional.ofNullable(properties.getLibraryFileNameSuffix()).orElse(".so")));
        }
        final OperatingSystemType osType = DefaultOperatingSystemTypes.detect();
        final String explicitArchitecture = properties.getArchitectureName();
        final ArchitectureType architectureType = (explicitArchitecture != null) ? (() -> explicitArchitecture) : DefaultArchitectureTypes.detect();
        return new SystemType(architectureType, osType);
    }
    
    private static class UnknownOperatingSystem implements OperatingSystemType
    {
        private final String libraryFilePrefix;
        private final String libraryFileSuffix;
        
        private UnknownOperatingSystem(final String libraryFilePrefix, final String libraryFileSuffix) {
            this.libraryFilePrefix = libraryFilePrefix;
            this.libraryFileSuffix = libraryFileSuffix;
        }
        
        @Override
        public String identifier() {
            return null;
        }
        
        @Override
        public String libraryFilePrefix() {
            return this.libraryFilePrefix;
        }
        
        @Override
        public String libraryFileSuffix() {
            return this.libraryFileSuffix;
        }
    }
}
