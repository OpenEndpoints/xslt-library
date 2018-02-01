package com.offerready.xslt.config;

import java.io.Serializable;

/** Just a string, but having a type makes static type checking better and increases readability. */
@SuppressWarnings("serial")
public class Privilege implements Comparable<Privilege>, Serializable {
    protected String name;
    public Privilege(String n) { name = n; }
    public String getName() { return name; }
    @Override public String toString() { return name; }
    @Override public boolean equals(Object obj) { return obj != null && ((Privilege)obj).name.equals(name); }
    @Override public int hashCode() { return name.hashCode(); }
    public int compareTo(Privilege o) { return name.compareTo(o.name); }
    /** For GWT only */ public Privilege() { }
}
