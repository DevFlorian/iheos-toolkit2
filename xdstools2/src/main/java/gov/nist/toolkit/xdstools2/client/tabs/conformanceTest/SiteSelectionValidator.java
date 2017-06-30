package gov.nist.toolkit.xdstools2.client.tabs.conformanceTest;

import gov.nist.toolkit.sitemanagementui.client.SiteSpec;

/**
 *
 */
public interface SiteSelectionValidator {

    /**
     * Offers popup if siteSpec does not include the right actor
     * @param siteSpec
     */
    void validate(SiteSpec siteSpec);
}
