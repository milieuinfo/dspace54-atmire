package com.atmire.access.model;

import org.junit.Test;

import javax.xml.bind.JAXBException;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author philip at atmire.com
 */
public class EpersonValueExtractorTest {

    @Test
    public void testMatchPartialValue() throws IOException, JAXBException {
        EpersonValueExtractor epersonValueExtractor = new EpersonValueExtractor();
        epersonValueExtractor.setValue("NISCODE:(.*):.*");

        String value = "NISCODE:12007:Gemeente Bornem";
        String epersonMatchedValue = epersonValueExtractor.extractEpersonAclValue(value);

        assertEquals("12007", epersonMatchedValue);
    }

    @Test
    public void testMatchFullValue() throws IOException, JAXBException {
        EpersonValueExtractor epersonValueExtractor = new EpersonValueExtractor();
        epersonValueExtractor.setValue("(.*)");

        String value = "NISCODE:12007:Gemeente Bornem";
        String epersonMatchedValue = epersonValueExtractor.extractEpersonAclValue(value);

        assertEquals("NISCODE:12007:Gemeente Bornem", epersonMatchedValue);
    }

    @Test
    public void testNoMatch() throws IOException, JAXBException {
        EpersonValueExtractor epersonValueExtractor = new EpersonValueExtractor();
        epersonValueExtractor.setValue("NISCODE:(.*):.*");

        String value = "NOISECODE:12007:Gemeente Bornem";
        String epersonMatchedValue = epersonValueExtractor.extractEpersonAclValue(value);

        assertNotEquals("12007", epersonMatchedValue);
        assertNull(epersonMatchedValue);
    }

    @Test
    public void testEmptyValue() throws IOException, JAXBException {
        EpersonValueExtractor epersonValueExtractor = new EpersonValueExtractor();
        epersonValueExtractor.setValue("NISCODE:(.*):.*");

        String value = "";
        String epersonMatchedValue = epersonValueExtractor.extractEpersonAclValue(value);

        assertNull(epersonMatchedValue);
    }
}
