package com.offerready.xslt.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Element;

import com.databasesandlife.util.DomParser;
import com.databasesandlife.util.gwtsafe.ConfigurationException;

import javax.annotation.Nonnull;

public class OfferReadyDomParser extends DomParser {
    
    /** @return a PrivilegeRestriction allowing all users, if tag is missing */
    protected static @Nonnull PrivilegeRestriction parsePrivilegeRestriction(@Nonnull Element container) throws ConfigurationException {
        List<Element> n = getSubElements(container, "privilege-restriction");
        if (n.size() > 1) throw new ConfigurationException("Too many <privilege-restriction> elements");
        if (n.isEmpty()) return PrivilegeRestriction.allowAny();
        Element p = n.get(0);

        PrivilegeRestriction.Type type;
        if ("all-but".equals(p.getAttribute("type"))) type = PrivilegeRestriction.Type.AllBut;
        else if ("none-but".equals(p.getAttribute("type"))) type = PrivilegeRestriction.Type.NoneBut;
        else throw new ConfigurationException("<privilege-restriction>: didn't understand type='" + p.getAttribute("type") + "'");

        Set<Privilege> privileges = new HashSet<Privilege>();
        for (Element pp : getSubElements(p, "privilege")) {
            String privName = pp.getAttribute("name");
            if (privName.equals("")) throw new ConfigurationException("Each <privilege> must have a name attribute");
            privileges.add(new Privilege(privName));
        }

        return new PrivilegeRestriction(type, privileges);
    }
}
