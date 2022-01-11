package me.justapie.lava.common.natives;

import java.io.InputStream;
import me.justapie.lava.common.natives.architecture.SystemType;

public interface NativeLibraryBinaryProvider
{
    InputStream getLibraryStream(final SystemType p0, final String p1);
}
