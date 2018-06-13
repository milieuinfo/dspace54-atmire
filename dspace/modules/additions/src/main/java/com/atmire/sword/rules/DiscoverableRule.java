package com.atmire.sword.rules;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;

/**
 * Validation rule that checks if an item is discoverable by an anonymous user through the DSpace search functionality.
 */
public class DiscoverableRule extends AbstractComplianceRule {

    private SearchService searchService;

    public DiscoverableRule(final SearchService searchService) {
        this.searchService = searchService;
    }

    protected String getRuleDescriptionCompliant() {
        return "the item is discoverable using the search functionality";
    }

    protected String getRuleDescriptionViolation() {
        return "the item must be discoverable using the search functionality";
    }

    protected boolean doValidationAndBuildDescription(final Context context, final Item item) {
        boolean valid = false;

        DiscoverQuery query = new DiscoverQuery();
        query.setQuery("handle:\"" + item.getHandle() + "\"");
        query.setMaxResults(0);
        query.setStart(0);

        try {
            Context anonymousContext = new Context();
            DiscoverResult result = searchService.search(anonymousContext, query);
            if(result != null && result.getTotalSearchResults() > 0) {
                valid = true;
            } else {
                addViolationDescription(item);
            }

            anonymousContext.complete();

        } catch (SearchServiceException e) {
            addViolationDescription("niet in staat om in discovery te zoeken naar item %s: %s", item.getHandle(), e.getMessage());
        } catch (SQLException e) {
            addViolationDescription("niet in staat om een anonieme context aan te maken voor het ondervragen van discovery voor item %s: %s", item.getHandle(), e.getMessage());
        }

        return valid;
    }

    private void addViolationDescription(final Item item) {
        String description = "item met %s %s is niet vindbaar via de zoekfunctionaliteit";
        if(item.getHandle() == null) {
            addViolationDescription(description, "titel", "\"" + item.getMetadata("dc.title") + "\"");
        } else {
            addViolationDescription(description, "handle", item.getHandle());
        }
    }
}
