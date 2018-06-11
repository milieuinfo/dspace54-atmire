package com.atmire.vocabulary;

import org.dspace.content.Metadatum;
import org.dspace.utils.DSpace;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 08 Jun 2018
 */
public class VocabularyUtils {

    public static List<String> getDCInputPairs(
            String fieldName,
            Metadatum[] dcValues,
            String vocabulary
    ) {
        List<String> valuesForField = getValuesForField(
                fieldName.replaceAll("_", "."), dcValues, vocabulary);
        return toPairs(valuesForField);
    }

    public static List<String> getValuesForField(
            String fieldName,
            Metadatum[] dcValues,
            String vocabulary
    ) {
        Field field = new DSpace().getServiceManager()
                .getServiceByName(vocabulary, Field.class);
        return getValuesForField(fieldName, dcValues, field);
    }

    private static List<String> getValuesForField(
            String fieldName,
            Metadatum[] dcValues,
            Field field
    ) {
        List<String> values = new ArrayList<>();
        if (field != null) {
            if (fieldName.equals(field.getField())) {
                values = getValues(field);
            } else {
                List<Value> matchingValues = getMatchingValue(field, dcValues);
                if (!matchingValues.isEmpty()) {
                    Iterator<Value> valueIterator = matchingValues.iterator();
                    while (valueIterator.hasNext() && values.isEmpty()) {
                        Value matchingValue = valueIterator.next();
                        Iterator<Field> fieldIterator = matchingValue.getFields().iterator();
                        while (fieldIterator.hasNext() && values.isEmpty()) {
                            Field nextField = fieldIterator.next();
                            values = getValuesForField(fieldName, dcValues, nextField);
                        }
                    }
                }
            }
        }
        return values;
    }

    private static List<String> toPairs(List<String> values) {
        List<String> pairs = new ArrayList<>();
        for (String value : values) {
            pairs.add(value); // display
            pairs.add(value); // stored value
        }
        return pairs;
    }

    private static List<Value> getMatchingValue(Field field, Metadatum[] dcValues) {
        List<Value> matchingValues = new ArrayList<>();
        for (Metadatum dcValue : dcValues) {
            if (dcValue.getField().equals(field.getField())) {
                for (Value value : field.getValues()) {
                    if (value.getValue().equalsIgnoreCase(dcValue.value)) {
                        matchingValues.add(value);
                    }
                }
            }
        }
        return matchingValues;
    }

    private static List<String> getValues(Field field) {
        List<String> values = new ArrayList<>();
        for (Value value : field.getValues()) {
            values.add(value.getValue());
        }
        return values;
    }


}
