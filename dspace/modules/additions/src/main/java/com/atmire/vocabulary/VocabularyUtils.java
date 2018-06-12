package com.atmire.vocabulary;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Metadatum;
import org.dspace.utils.DSpace;

import java.util.ArrayList;
import java.util.Collections;
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
        return getValuesForField(fieldName, dcValues, field, 0).getRight();
    }

    private static Pair<Integer, List<String>> getValuesForField(
            String fieldName,
            Metadatum[] dcValues,
            Field field,
            int depth
    ) {
        Pair<Integer, List<String>> result = Pair.of(depth, Collections.<String>emptyList());
        if (field != null) {
            if (fieldName.equals(field.getField())) {
                result = Pair.of(depth, getValues(field));
            } else {
                Value matchingValue = getMatchingValue(field, dcValues);
                for (Field nextField : getFields(matchingValue)) {
                    Pair<Integer, List<String>> values
                            = getValuesForField(fieldName, dcValues, nextField, depth + 1);
                    if (values.getLeft() > result.getLeft()) {
                        result = values;
                    }
                }
            }
        }
        return result;
    }

    private static List<Field> getFields(Value value) {
        if (value != null && value.getFields() != null) {
            return value.getFields();
        } else {
            return Collections.emptyList();
        }
    }

    private static List<String> toPairs(List<String> values) {
        List<String> pairs = new ArrayList<>();
        for (String value : values) {
            pairs.add(value); // display
            pairs.add(value); // stored value
        }
        return pairs;
    }

    private static Value getMatchingValue(Field field, Metadatum[] dcValues) {
        for (Metadatum dcValue : dcValues) {
            if (dcValue.getField().equals(field.getField())) {
                for (Value value : field.getValues()) {
                    if (value.getValue().equalsIgnoreCase(dcValue.value)) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private static List<String> getValues(Field field) {
        List<String> values = new ArrayList<>();
        for (Value value : field.getValues()) {
            values.add(value.getValue());
        }
        return values;
    }


}
