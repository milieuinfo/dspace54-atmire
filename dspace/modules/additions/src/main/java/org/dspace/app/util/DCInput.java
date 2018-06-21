/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import com.atmire.utils.ItemUtils;
import com.atmire.utils.helper.MetadataFieldString;
import com.atmire.utils.subclasses.MetadatumExtended;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class representing a line in an input form.
 * 
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
 */
public class DCInput
{
    /** the DC element name */
    private String dcElement = null;

    /** the DC qualifier, if any */
    private String dcQualifier = null;

    /** the DC namespace schema */
    private String dcSchema = null;

    /** a label describing input */
    private String label = null;

    /** the input type */
    private String inputType = null;

    /** is input required? */
    private boolean required = false;

    /** if required, text to display when missing */
    private String warning = null;

    /** is input repeatable? */
    private boolean repeatable = false;

    /** 'hint' text to display */
    private String hint = null;

    /** if input list-controlled, name of list */
    private String valueListName = null;

    /** if input list-controlled, the list itself */
    private List<String> valueList = null;

    /** if non-null, visibility scope restriction */
    private String visibility = null;
    
    /** if non-null, readonly out of the visibility scope */
    private String readOnly = null;

    /** the name of the controlled vocabulary to use */
    private String vocabulary = null;

    /** is the entry closed to vocabulary terms? */
    private boolean closedVocabulary = false;

    /** allowed document types */
    private Map<String, List<String>> typeBind = null;

    private boolean displayOnly;

    /** 
     * The scope of the input sets, this restricts hidden metadata fields from 
     * view during workflow processing. 
     */
    public static final String WORKFLOW_SCOPE = "workflow";

    /** 
     * The scope of the input sets, this restricts hidden metadata fields from 
     * view by the end user during submission. 
     */
    public static final String SUBMISSION_SCOPE = "submit";
    
    /**
     * Class constructor for creating a DCInput object based on the contents of
     * a HashMap
     * 
     * @param fieldMap
     *            ???
     * @param listMap
     */
    public DCInput(Map<String, String> fieldMap, Map<String, List<String>> listMap)
    {
        dcElement = fieldMap.get("dc-element");
        dcQualifier = fieldMap.get("dc-qualifier");

        // Default the schema to dublin core
        dcSchema = fieldMap.get("dc-schema");
        if (dcSchema == null)
        {
            dcSchema = MetadataSchema.DC_SCHEMA;
        }

        String repStr = fieldMap.get("repeatable");
        repeatable = "true".equalsIgnoreCase(repStr)
                || "yes".equalsIgnoreCase(repStr);
        label = fieldMap.get("label");
        inputType = fieldMap.get("input-type");
        // these types are list-controlled
        if ("dropdown".equals(inputType) || "qualdrop_value".equals(inputType)
                || "list".equals(inputType))
        {
            valueListName = fieldMap.get("value-pairs-name");
            valueList = listMap.get(valueListName);
        }
        hint = fieldMap.get("hint");
        warning = fieldMap.get("required");
        required = (warning != null && warning.length() > 0);
        visibility = fieldMap.get("visibility");
        readOnly = fieldMap.get("readonly");
        vocabulary = fieldMap.get("vocabulary");
        String closedVocabularyStr = fieldMap.get("closedVocabulary");
        closedVocabulary = "true".equalsIgnoreCase(closedVocabularyStr)
                            || "yes".equalsIgnoreCase(closedVocabularyStr);
        
        // parsing of the <type-bind> element
        typeBind = new HashMap<String, List<String>>();
        fillTypeBinds(fieldMap, typeBind, "type-bind");
        displayOnly = "true".equalsIgnoreCase(fieldMap.get("display-only"));
        
    }

    public void fillTypeBinds(Map<String, String> fieldMap, Map<String, List<String>> typeBind, String nodeName) {
        String allTypeBinds = fieldMap.get(nodeName+"_all");
        if (StringUtils.isNotBlank(allTypeBinds)) {
            String[] typeBinds = allTypeBinds.split(" ");
            for (String bind : typeBinds) {
                List<String> typeList = new ArrayList<String>();
                String typeBindDef = fieldMap.get(nodeName+"#" + bind);
                if (typeBindDef != null && typeBindDef.trim().length() > 0) {
                    String[] types = typeBindDef.split(";"); // changed the type bind separator from a comma to a semicolon
                    for (String type : types) {
                        typeList.add(type.trim());
                    }
                }
                typeBind.put(bind, typeList);
            }
        }
    }

    /**
     * Is this DCInput for display in the given scope? The scope should be
     * either "workflow" or "submit", as per the input forms definition. If the
     * internal visibility is set to "null" then this will always return true.
     * 
     * @param scope
     *            String identifying the scope that this input's visibility
     *            should be tested for
     * 
     * @return whether the input should be displayed or not
     */
    public boolean isVisible(String scope)
    {
        return (visibility == null || visibility.equals(scope));
    }
    
    /**
     * Is this DCInput for display in readonly mode in the given scope? 
     * If the scope differ from which in visibility field then we use the out attribute
     * of the visibility element. Possible values are: hidden (default) and readonly.
     * If the DCInput is visible in the scope then this methods must return false
     * 
     * @param scope
     *            String identifying the scope that this input's readonly visibility
     *            should be tested for
     * 
     * @return whether the input should be displayed in a readonly way or fully hidden
     */
    public boolean isReadOnly(String scope)
    {
        if (isVisible(scope))
        {
            return false;
        }
        else
        {
            return readOnly != null && readOnly.equalsIgnoreCase("readonly");
        }
    }


    /**
     * Get the repeatable flag for this row
     * 
     * @return the repeatable flag
     */
    public boolean isRepeatable()
    {
        return repeatable;
    }

    /**
     * Alternate way of calling isRepeatable()
     * 
     * @return the repeatable flag
     */
    public boolean getRepeatable()
    {
        return isRepeatable();
    }

    /**
     * Get the input type for this row
     * 
     * @return the input type
     */
    public String getInputType()
    {
        return inputType;
    }

    /**
     * Get the DC element for this form row.
     * 
     * @return the DC element
     */
    public String getElement()
    {
        return dcElement;
    }

    /**
     * Get the DC namespace prefix for this form row.
     * 
     * @return the DC namespace prefix
     */
    public String getSchema()
    {
        return dcSchema;
    }

    /**
     * Get the warning string for a missing required field, formatted for an
     * HTML table.
     * 
     * @return the string prompt if required field was ignored
     */
    public String getWarning()
    {
        return warning;
    }

    /**
     * Is there a required string for this form row?
     * 
     * @return true if a required string is set
     */
    public boolean isRequired()
    {
        return required;
    }

    /**
     * Get the DC qualifier for this form row.
     * 
     * @return the DC qualifier
     */
    public String getQualifier()
    {
        return dcQualifier;
    }

    /**
     * Get the hint for this form row, formatted for an HTML table
     * 
     * @return the hints
     */
    public String getHints()
    {
        return hint;
    }

    /**
     * Get the label for this form row.
     * 
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Get the name of the pairs type
     * 
     * @return the pairs type name
     */
    public String getPairsType()
    {
        return valueListName;
    }

    /**
     * Get the name of the pairs type
     * 
     * @return the pairs type name
     */
    public List getPairs()
    {
        return valueList;
    }

    public void setPairs(List<String> pairs){
        valueList = pairs;
    }

    /**
     * Get the name of the controlled vocabulary that is associated with this
     * field
     * 
     * @return the name of associated the vocabulary
     */
    public String getVocabulary()
    {
        return vocabulary;
    }

    /**
     * Set the name of the controlled vocabulary that is associated with this
     * field
     * 
     * @param vocabulary
     *            the name of the vocabulary
     */
    public void setVocabulary(String vocabulary)
    {
        this.vocabulary = vocabulary;
    }

    /**
     * Gets the display string that corresponds to the passed storage string in
     * a particular display-storage pair set.
     * 
     * @param pairTypeName
     *            Name of display-storage pair set to search
     * @param storedString
     *            the string that gets stored
     * 
     * @return the displayed string whose selection causes storageString to be
     *         stored, null if no match
     */
    public String getDisplayString(String pairTypeName, String storedString)
    {
        if (valueList != null && storedString != null)
        {
            for (int i = 0; i < valueList.size(); i += 2)
            {
                if (storedString.equals(valueList.get(i + 1)))
                {
                    return valueList.get(i);
                }
            }
        }
        return null;
    }

    /**
     * Gets the stored string that corresponds to the passed display string in a
     * particular display-storage pair set.
     * 
     * @param pairTypeName
     *            Name of display-storage pair set to search
     * @param displayedString
     *            the string that gets displayed
     * 
     * @return the string that gets stored when displayString gets selected,
     *         null if no match
     */
    public String getStoredString(String pairTypeName, String displayedString)
    {
        if (valueList != null && displayedString != null)
        {
            for (int i = 0; i < valueList.size(); i += 2)
            {
                if (displayedString.equals(valueList.get(i)))
                {
                    return valueList.get(i + 1);
                }
            }
        }
        return null;
    }

	/**
	 * The closed attribute of the vocabulary tag for this field as set in 
	 * input-forms.xml
	 * 
	 * <code> 
	 * <field>
	 *     .....
	 *     <vocabulary closed="true">nsrc</vocabulary>
	 * </field>
	 * </code>
	 * @return the closedVocabulary flags: true if the entry should be restricted 
	 *         only to vocabulary terms, false otherwise
	 */
	public boolean isClosedVocabulary() {
		return closedVocabulary;
	}

    public boolean isAllowedFor(Item item) {
        return isAllowedForTypeBindSet(item, typeBind);
    }

    private boolean isAllowedForTypeBindSet(Item item, Map<String, List<String>> typeBind) {
        Map<String, String> typebindValues = new HashMap<String, String>();
        for (String field : typeBind.keySet()) {
            String value = getMetadataFirstValue(item, field);
            typebindValues.put(field, value);
        }
        return isAllowedForTypeBindSet(typebindValues, typeBind);
    }

    public static String getMetadataFirstValue(Item item, String fieldName) {
        MetadatumExtended elements
                = MetadataFieldString.encapsulate(fieldName);
        return ItemUtils.getMetadataFirstValue(
                item,
                elements.getSchema(),
                elements.getElement(),
                elements.getQualifier(),
                Item.ANY // important distinction with ItemUtils.getMetadataFirstValue
        );
    }

    /**
     * Decides if this field is valid for the document type
     *
     * @param typebindValues mapping of fields - values
     * @return true when there is no type restriction or typeName is allowed
     */
    protected boolean isAllowedForTypeBindSet(
            Map<String, String> typebindValues,
            Map<String, List<String>> typeBind
    ) {
        boolean allowed = true;

        if (typebindValues == null || typebindValues.size() < typeBind.keySet().size()) {
            allowed = false;
        } else if (typeBind.size() > 0) {
            for (Map.Entry<String, String> entry : typebindValues.entrySet()) {
                String field = entry.getKey();
                String value = entry.getValue();
                if (!typeBind.get(field).contains(value)) {
                    allowed = false;
                }
            }
        }

        return allowed;
    }

    public boolean isDisplayOnly() {
        return displayOnly;
    }
}
