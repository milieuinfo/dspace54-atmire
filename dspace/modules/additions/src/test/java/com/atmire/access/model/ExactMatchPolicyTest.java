package com.atmire.access.model;

import java.io.*;
import javax.xml.bind.*;
import static org.junit.Assert.*;
import org.junit.*;

/**
 * @author philip at atmire.com
 */
public class ExactMatchPolicyTest {

    private ExactMatchPolicy exactMatchPolicy;

    @Before
    public void setUp() {
        exactMatchPolicy = new ExactMatchPolicy();
    }

    @Test
    public void testMatchPartialValue() throws IOException, JAXBException {
        EpersonValueMatcher epersonValueMatcher = new EpersonValueMatcher();
        epersonValueMatcher.setValue("NISCODE:(.*):.*");
        exactMatchPolicy.setEpersonValueMatcher(epersonValueMatcher);

        String value = "NISCODE:12007:Gemeente Bornem";
        String epersonMatchedValue = exactMatchPolicy.getEpersonMatchedValue(value);

        assertEquals("12007", epersonMatchedValue);
    }

    @Test
    public void testMatchFullValue() throws IOException, JAXBException {
        EpersonValueMatcher epersonValueMatcher = new EpersonValueMatcher();
        epersonValueMatcher.setValue("(.*)");
        exactMatchPolicy.setEpersonValueMatcher(epersonValueMatcher);

        String value = "NISCODE:12007:Gemeente Bornem";
        String epersonMatchedValue = exactMatchPolicy.getEpersonMatchedValue(value);

        assertEquals("NISCODE:12007:Gemeente Bornem", epersonMatchedValue);
    }

    @Test
    public void testNoMatch() throws IOException, JAXBException {
        EpersonValueMatcher epersonValueMatcher = new EpersonValueMatcher();
        epersonValueMatcher.setValue("NISCODE:(.*):.*");
        exactMatchPolicy.setEpersonValueMatcher(epersonValueMatcher);

        String value = "NOISECODE:12007:Gemeente Bornem";
        String epersonMatchedValue = exactMatchPolicy.getEpersonMatchedValue(value);

        assertNotEquals("12007", epersonMatchedValue);
    }

    @Test
    public void testEmptyValue() throws IOException, JAXBException {
        EpersonValueMatcher epersonValueMatcher = new EpersonValueMatcher();
        epersonValueMatcher.setValue("NISCODE:(.*):.*");
        exactMatchPolicy.setEpersonValueMatcher(epersonValueMatcher);

        String value = "";
        String epersonMatchedValue = exactMatchPolicy.getEpersonMatchedValue(value);

        assertNull(epersonMatchedValue);
    }
}
