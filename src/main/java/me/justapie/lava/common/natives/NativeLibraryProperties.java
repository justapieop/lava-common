package me.justapie.lava.common.natives;

public interface NativeLibraryProperties
{
    String getLibraryPath();
    
    String getLibraryDirectory();
    
    String getExtractionPath();
    
    String getSystemName();
    
    String getLibraryFileNamePrefix();
    
    String getLibraryFileNameSuffix();
    
    String getArchitectureName();
}
