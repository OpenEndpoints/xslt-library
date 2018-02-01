package com.offerready.xslt.config;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

@SuppressWarnings("serial")
public class PrivilegeRestriction implements Serializable {

    public enum Type { AllBut, NoneBut };

    public interface HasPrivilegeRestriction {
        public PrivilegeRestriction getPrivilegeRestriction();
    }

    public Type type;
    public Set<Privilege> privileges;

    protected PrivilegeRestriction() { }
    
    public static PrivilegeRestriction allowAny() {
        PrivilegeRestriction result = new PrivilegeRestriction();
        result.type = Type.AllBut;
        result.privileges = new HashSet<Privilege>();
        return result;
    }

    public PrivilegeRestriction(Type t, Set<Privilege> p) {
        type = t;
        privileges = p;
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
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

    public boolean isAllowed(Privilege p) {
        boolean contained = privileges.contains(p);
        return type==Type.NoneBut ? contained : !contained;
    }

    public static <P extends HasPrivilegeRestriction> List<P> restrict(Collection<P> input, Privilege privilege) {
        List<P> result = new Vector<P>(input.size());
        for (P candidate : input)
            if (candidate.getPrivilegeRestriction().isAllowed(privilege))
                result.add(candidate);
        return result;
    }

    /**
     * Checks that all privileges have at least one option, including an "other" privilge
     * @return privilege name which doesn't have an option, or null if all have options
     */
    public static Privilege checkListManagesAllCases(List<PrivilegeRestriction> l) {
        Set<Privilege> allPrivileges = new HashSet<Privilege>();
        allPrivileges.add(new Privilege("*other*"));
        for (PrivilegeRestriction restriction : l)
            allPrivileges.addAll(restriction.privileges);

        for (Privilege p : allPrivileges) {
            boolean allowed = false;
            for (PrivilegeRestriction restriction : l)
                if (restriction.isAllowed(p)) allowed = true;
            if ( ! allowed) return p;
        }

        return null;
    }
}
