package com.atmire.access.model;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * @author philip at atmire.com
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "metadata-based-access-control-policies")
public class MetadataBasedAccessControlPolicies {

    @XmlElement(name="group-policy")
    private List<GroupPolicy> groupPolicies;

    public List<GroupPolicy> getGroupPolicies() {
        return groupPolicies;
    }

    public void setGroupPolicies(List<GroupPolicy> groupPolicies) {
        this.groupPolicies = groupPolicies;
    }
}
