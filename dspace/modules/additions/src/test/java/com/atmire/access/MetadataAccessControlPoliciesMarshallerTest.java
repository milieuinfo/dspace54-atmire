package com.atmire.access;

import com.atmire.access.model.*;
import java.io.*;
import javax.xml.bind.*;
import org.apache.commons.io.*;
import org.junit.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author philip at atmire.com
 */
public class MetadataAccessControlPoliciesMarshallerTest {

    private MetadataAccessControlPoliciesMarshaller marshaller;

    @Before
    public void setUp() {
        marshaller = new MetadataAccessControlPoliciesMarshaller();
    }

    @Test
    public void testUnmarshall() throws IOException, JAXBException {
        File file = FileUtils.toFile(MetadataAccessControlPoliciesMarshallerTest.class.getResource("metadata-based-access-control.xml"));
        FileInputStream inputStream = new FileInputStream(file);

        MetadataBasedAccessControlPolicies metadataBasedAccessControlPolicies = marshaller.unmarshal(inputStream);
        inputStream.close();

        assertNotNull(metadataBasedAccessControlPolicies);
        assertEquals(2, metadataBasedAccessControlPolicies.getGroupPolicies().size());

        GroupPolicy groupPolicy = metadataBasedAccessControlPolicies.getGroupPolicies().get(0);

        assertEquals("DBA_Eigen_Dossiers_Lezen", groupPolicy.getGroupName());
        assertEquals(2, groupPolicy.getExactMatchPolicies().size());

        ExactMatchPolicy exactMatchPolicy = groupPolicy.getExactMatchPolicies().get(0);

        assertEquals("dba.vvo.nis", exactMatchPolicy.getItemField().getValue());
        assertEquals("acl.field1", exactMatchPolicy.getEpersonField().getValue());
    }
}
