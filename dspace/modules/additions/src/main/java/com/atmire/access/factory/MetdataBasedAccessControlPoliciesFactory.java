package com.atmire.access.factory;

import com.atmire.access.model.*;
import java.util.*;

/**
 * @author philip at atmire.com
 */
public interface MetdataBasedAccessControlPoliciesFactory {

    List<Policy> getPolicies(String groupName);
}
