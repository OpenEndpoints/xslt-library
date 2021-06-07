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
        var restrictionElements = getSubElements(container, "privilege-restriction");
        if (restrictionElements.size() > 1) throw new ConfigurationException("Too many <privilege-restriction> elements");
        if (restrictionElements.isEmpty()) return PrivilegeRestriction.allowAny();
        var el = restrictionElements.get(0);

        PrivilegeRestriction.Type type;
        if ("all-but".equals(el.getAttribute("type"))) type = PrivilegeRestriction.Type.AllBut;
        else if ("none-but".equals(el.getAttribute("type"))) type = PrivilegeRestriction.Type.NoneBut;
        else throw new ConfigurationException("<privilege-restriction>: didn't understand type='" + el.getAttribute("type") + "'");

        var privileges = new HashSet<Privilege>();
        for (var priv : getSubElements(el, "privilege")) {
            String privName = priv.getAttribute("name");
            if (privName.equals("")) throw new ConfigurationException("Each <privilege> must have a name attribute");
            privileges.add(new Privilege(privName));
        }

        return new PrivilegeRestriction(type, privileges);
    }
}
