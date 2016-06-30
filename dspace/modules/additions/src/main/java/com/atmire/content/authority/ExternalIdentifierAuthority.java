package com.atmire.content.authority;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;

/**
 * Created by dylan on 30/06/16.
 */
public class ExternalIdentifierAuthority implements ChoiceAuthority
{
    public Choices getMatches(String field, String text, int collection, int start, int limit, String locale)
    {
        // return as is
        Choices choices = new Choices(false);
        if(StringUtils.isNotBlank(text))
        {
            choices = new Choices(new Choice[]{new Choice("value_authority",text+"_value",text+"_label")},0,1,Choices.CF_AMBIGUOUS,false);
        }
        return choices;
    }

    public Choices getBestMatch(String field, String text, int collection, String locale)
    {
        Choices choices = new Choices(false);
        if(StringUtils.isNotBlank(text))
        {
            choices = new Choices(new Choice[]{new Choice("value_authority",text+"_value",text+"_label")},0,1,Choices.CF_AMBIGUOUS,false);
        }
        return choices;
    }

    public String getLabel(String field, String key, String locale)
    {
        return key;
    }
}
