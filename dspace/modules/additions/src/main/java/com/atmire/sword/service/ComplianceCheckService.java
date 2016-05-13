package com.atmire.sword.service;

import com.atmire.sword.result.*;
import org.dspace.content.*;
import org.dspace.core.*;

/**
 * Service to check if an item is compliant with the defined validation rules
 */
public interface ComplianceCheckService {

    ComplianceResult checkCompliance(final Context context, final Item item);
}
