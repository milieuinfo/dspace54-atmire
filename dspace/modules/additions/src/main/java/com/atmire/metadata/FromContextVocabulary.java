package com.atmire.metadata;

import com.atmire.utils.MetadataUtils;
import com.atmire.vocabulary.VocabularyUtils;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;

import java.util.List;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 07 Jun 2018
 */
public class FromContextVocabulary extends AbstractFillValue {

    private String vocabulary;

    public String getVocabulary() {
        return vocabulary;
    }

    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    @Override
    public List<Metadatum> getValues(EditParameters parameters) {
        Metadatum[] metadata = parameters.getObject().getMetadata(
                Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        List<String> valuesForField = VocabularyUtils.getValuesForField(
                getField(), metadata, getVocabulary());
        return convert(valuesForField);
    }

}
