package com.atmire.lne.swordv2;

import org.apache.commons.httpclient.HttpStatus;
import org.dspace.sword2.CollectionDepositManagerDSpace;
import org.dspace.sword2.DSpaceSwordAPI;
import org.swordapp.server.*;

/**
 * SWORD v2 collection deposit manager for the LNE deposit process. It makes use of the standard DSpace deposit manager.
 */
public class LneCollectionDepositManagerDspace extends DSpaceSwordAPI implements CollectionDepositManager {

    private CollectionDepositManagerDSpace depositManagerDSpace;

    public LneCollectionDepositManagerDspace() {
        depositManagerDSpace = new CollectionDepositManagerDSpace();
    }

    public DepositReceipt createNew(final String collectionUri, final Deposit deposit, final AuthCredentials authCredentials, final SwordConfiguration swordConfiguration) throws SwordError, SwordServerException, SwordAuthException {
        if(deposit.isInProgress()) {
            throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, HttpStatus.SC_METHOD_NOT_ALLOWED, "Requests with header In-Progress set to true are not allowed.");
        } else {
            return depositManagerDSpace.createNew(collectionUri, deposit, authCredentials, swordConfiguration);
        }
    }

}
