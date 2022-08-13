package com.offerready.xslt;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

import javax.annotation.Nonnull;
import java.io.Serializable;

@Data
@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
public class Privilege implements Comparable<Privilege>, Serializable {

    protected @Nonnull String name;

    public int compareTo(@Nonnull Privilege o) { return name.compareTo(o.name); }
}
