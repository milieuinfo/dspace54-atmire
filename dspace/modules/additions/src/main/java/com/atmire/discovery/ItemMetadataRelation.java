package com.atmire.discovery;

import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by jonas - jonas@atmire.com on 26/05/16.
 */
public class ItemMetadataRelation {

    private DiscoverySearchFilterFacet sourceFilterFacet;

    private DiscoverySearchFilterFacet destinationFilterFacet;

    private String sourceMetadataField;

    private String destinationMetadataField;

    private boolean inverseRelationSearchEnabled = true;

    public ItemMetadataRelation createInverseMetadataRelation(){
        ItemMetadataRelation metadataRelation = new ItemMetadataRelation();

        metadataRelation.setSourceMetadataField(getDestinationMetadataField());
        metadataRelation.setDestinationMetadataField(getSourceMetadataField());
        metadataRelation.setDestinationFilterFacet(getSourceFilterFacet());
        metadataRelation.setSourceFilterFacet(getDestinationFilterFacet());
        metadataRelation.setInverseRelationSearchEnabled(this.isInverseRelationSearchEnabled());

        return metadataRelation;
    }

    @Required
    public void setSourceMetadataField(String sourceMetadataField) {
        this.sourceMetadataField = sourceMetadataField;
    }

    @Required
    public void setDestinationMetadataField(String destinationMetadataField) {
        this.destinationMetadataField = destinationMetadataField;
    }

    public void setInverseRelationSearchEnabled(boolean inverseRelationSearchEnabled) {
        this.inverseRelationSearchEnabled = inverseRelationSearchEnabled;
    }
    public String getSourceMetadataField() {
        return sourceMetadataField;
    }

    public String getDestinationMetadataField() {
        return destinationMetadataField;
    }

    public boolean isInverseRelationSearchEnabled() {
        return inverseRelationSearchEnabled;
    }

    public DiscoverySearchFilterFacet getDestinationFilterFacet() {
        return destinationFilterFacet;
    }
    @Required
    public void setDestinationFilterFacet(DiscoverySearchFilterFacet destinationFilterFacet) {
        this.destinationFilterFacet = destinationFilterFacet;
    }

    public DiscoverySearchFilterFacet getSourceFilterFacet() {
        return sourceFilterFacet;
    }
    @Required
    public void setSourceFilterFacet(DiscoverySearchFilterFacet sourceFilterFacet) {
        this.sourceFilterFacet = sourceFilterFacet;
    }

}
