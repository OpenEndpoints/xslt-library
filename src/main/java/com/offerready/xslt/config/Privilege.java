package com.offerready.xslt.config;

import javax.annotation.Nonnull;
import java.io.Serializable;

/** Just a string, but having a type makes static type checking better and increases readability. */
@SuppressWarnings("serial")
public class Privilege implements Comparable<Privilege>, Serializable {
    protected @Nonnull String name;
    public Privilege(@Nonnull String n) { name = n; }
    public @Nonnull String getName() { return name; }
    @Override @Nonnull public String toString() { return name; }
    @Override public boolean equals(Object obj) { return obj instanceof Privilege && ((Privilege)obj).name.equals(name); }
    @Override public int hashCode() { return name.hashCode(); }
    public int compareTo(@Nonnull Privilege o) { return name.compareTo(o.name); }
}
