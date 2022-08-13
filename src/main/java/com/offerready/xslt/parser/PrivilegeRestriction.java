package com.offerready.xslt.parser;

import com.offerready.xslt.Privilege;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.*;

@SuppressWarnings("serial")
public class PrivilegeRestriction implements Serializable {

    public enum Type { AllBut, NoneBut };

    public interface HasPrivilegeRestriction {
        public PrivilegeRestriction getPrivilegeRestriction();
    }

    public @Nonnull Type type;
    public @Nonnull Set<Privilege> privileges;

    public PrivilegeRestriction(@Nonnull Type t, @Nonnull Set<Privilege> p) {
        type = t;
        privileges = p;
    }

    public static @Nonnull PrivilegeRestriction allowAny() {
        return new PrivilegeRestriction(Type.AllBut, Set.of());
    }

    public @Nonnull String toString() {
        var result = new StringBuilder();
        result.append(type.toString());
        result.append("[");
        boolean first = true;
        for (Privilege p : privileges) {
            if ( ! first) result.append(",");
            first = false;
            result.append(p);
        }
        result.append("]");
        return result.toString();
    }

    public boolean isAllowed(@Nonnull Privilege p) {
        boolean contained = privileges.contains(p);
        return type==Type.NoneBut ? contained : !contained;
    }

    public static @Nonnull <P extends HasPrivilegeRestriction> List<P> restrict(@Nonnull Collection<P> input, @Nonnull Privilege privilege) {
        var result = new ArrayList<P>(input.size());
        for (var candidate : input)
            if (candidate.getPrivilegeRestriction().isAllowed(privilege))
                result.add(candidate);
        return result;
    }

    /**
     * Checks that all privileges have at least one option, including an "other" privilge
     * @return privilege name which doesn't have an option, or null if all have options
     */
    public static @CheckForNull Privilege checkListManagesAllCases(@Nonnull List<PrivilegeRestriction> l) {
        var allPrivileges = new HashSet<Privilege>();
        allPrivileges.add(new Privilege("*other*"));
        for (var restriction : l)
            allPrivileges.addAll(restriction.privileges);

        for (var p : allPrivileges) {
            boolean allowed = false;
            for (PrivilegeRestriction restriction : l)
                if (restriction.isAllowed(p)) allowed = true;
            if ( ! allowed) return p;
        }

        return null;
    }
}
