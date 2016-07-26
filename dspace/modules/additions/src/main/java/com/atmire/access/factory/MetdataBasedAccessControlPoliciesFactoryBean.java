package com.atmire.access.factory;

import com.atmire.access.*;
import com.atmire.access.model.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

/**
 * @author philip at atmire.com
 */
public class MetdataBasedAccessControlPoliciesFactoryBean implements MetdataBasedAccessControlPoliciesFactory {

    private static Logger log = Logger.getLogger(MetdataBasedAccessControlPoliciesFactoryBean.class);

    private String filePath;

    private Map<String, List<? extends Policy>> policies;
    private long lastModified;

    public List<? extends Policy> getPolicies(String groupName) {
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
                    policies.put(groupPolicy.getGroupName(), groupPolicy.getExactMatchPolicies());
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

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
