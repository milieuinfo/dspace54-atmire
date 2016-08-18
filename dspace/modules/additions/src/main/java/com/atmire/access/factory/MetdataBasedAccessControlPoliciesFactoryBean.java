package com.atmire.access.factory;

import com.atmire.access.MetadataAccessControlPoliciesMarshaller;
import com.atmire.access.model.ExactMatchPolicy;
import com.atmire.access.model.GroupPolicy;
import com.atmire.access.model.MetadataBasedAccessControlPolicies;
import com.atmire.access.model.Policy;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author philip at atmire.com
 */
public class MetdataBasedAccessControlPoliciesFactoryBean implements MetdataBasedAccessControlPoliciesFactory {

    private static Logger log = Logger.getLogger(MetdataBasedAccessControlPoliciesFactoryBean.class);

    private String filePath;

    private Map<String, List<Policy>> policies;
    private long lastModified;

    public List<Policy> getPolicies(String groupName) {
        updatePolicies();

        if(policies.containsKey(groupName)) {
            return policies.get(groupName);
        }
        return new ArrayList<>();
    }

    private void updatePolicies(){
        FileInputStream inputStream = null;
        try {
            File file = new File(filePath);

            if(lastModified != file.lastModified()) {
                policies = new HashMap<>();
                inputStream = new FileInputStream(file);

                MetadataAccessControlPoliciesMarshaller marshaller = new MetadataAccessControlPoliciesMarshaller();
                MetadataBasedAccessControlPolicies metadataBasedAccessControlPolicies = marshaller.unmarshal(inputStream);

                for (GroupPolicy groupPolicy : metadataBasedAccessControlPolicies.getGroupPolicies()) {
                    policies.put(groupPolicy.getGroupName(), toPolicyList(groupPolicy.getExactMatchPolicies()));
                }

                lastModified = file.lastModified();
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private List<Policy> toPolicyList(final List<ExactMatchPolicy> exactMatchPolicies) {
        List<Policy> output = new LinkedList<>();
        if(CollectionUtils.isNotEmpty(exactMatchPolicies)) {
            for (ExactMatchPolicy exactMatchPolicy : exactMatchPolicies) {
                output.add(exactMatchPolicy);
            }
        }

        return output;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
