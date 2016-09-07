package com.atmire.access.model;

import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author philip at atmire.com
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "epersonValueExtractor")
public class EpersonValueExtractor {

    @XmlValue
    private String value;

    @XmlTransient
    private Pattern pattern = null;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String extractEpersonAclValue(String rawValue) {
        String matchedValue = null;

        if(getPattern() != null) {
            Matcher matcher = getPattern().matcher(rawValue);

            if (matcher.find()) {
                matchedValue = matcher.group(1);
            }
        }

        return matchedValue;
    }

    private Pattern getPattern() {
        if(pattern == null && StringUtils.isNotBlank(value)) {
            pattern = Pattern.compile(value);
        }
        return pattern;
    }
}
