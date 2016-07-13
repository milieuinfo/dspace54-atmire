package com.atmire.access.model;

import java.util.*;
import javax.xml.bind.annotation.*;

/**
 * @author philip at atmire.com
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "group-policy")
public class GroupPolicy {

    @XmlElement(name="exact-match-policy")
    private List<ExactMatchPolicy> exactMatchPolicies;

    @XmlAttribute(name = "groupName", required = true)
    private String groupName;

    public List<ExactMatchPolicy> getExactMatchPolicies() {
        return exactMatchPolicies;
    }

    public void setExactMatchPolicies(List<ExactMatchPolicy> exactMatchPolicies) {
        this.exactMatchPolicies = exactMatchPolicies;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
