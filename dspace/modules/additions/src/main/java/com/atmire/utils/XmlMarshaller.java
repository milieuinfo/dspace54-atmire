package com.atmire.utils;

import java.io.*;
import javax.xml.bind.*;
import javax.xml.namespace.*;
import javax.xml.transform.stream.*;

/**
 * Utility class to marshall or unmarshall object to/from XML
 */
public class XmlMarshaller<T> {

    private Class<T> clazz;

    public XmlMarshaller(final Class<T> clazz) {
        this.clazz = clazz;
    }

    public T unmarshal(final InputStream xmlContent) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return unmarshaller.unmarshal(new StreamSource(xmlContent), clazz).getValue();
    }

    public String marshall(final T objectToTransform)
            throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(clazz);
        Marshaller marshaller = context.createMarshaller();
        StringWriter sw = new StringWriter();
        marshaller
                .marshal(new JAXBElement<T>(new QName(clazz.getClass().getSimpleName()), clazz, objectToTransform), sw);
        return sw.toString();
    }
}

