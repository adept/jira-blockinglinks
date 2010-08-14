/*
 * Copyright (c) 2002-2004 Alex Perez <quimicefa@gmail.com>
 * Copyright (c) 2010 Dmitry Astapov <dastapov@gmail.com>
 * All rights reserved.
 */
package ua.astapov.jira.plugins.blockinglinks;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.link.IssueLink;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.Condition;
import org.apache.log4j.Logger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class BlockingLinksCondition implements Condition
{
    private static final Logger log = Logger.getLogger(BlockingLinksCondition.class);

    @SuppressWarnings("rawtypes")
	public boolean passesCondition(Map transientVars, Map args, PropertySet ps)
    {
        // actual issue
        Issue issue = (Issue) transientVars.get("issue");

        // allowed states
        String statuses = (String) args.get("statuses");
        log.debug("statuses: " + statuses);
        String linkTypes = (String) args.get("linkTypes");
        log.debug("linkTypes: " + linkTypes);

        List<String> validStates = Arrays.asList( statuses.split(","));
        List<String> inflictedLinkTypes = Arrays.asList( linkTypes.split(","));

        // incidents linked to this one
        List<IssueLink> inwardLinks = ComponentManager.getInstance().getIssueLinkManager().getInwardLinks(issue.getLong("id"));

        for (int i = 0; i < inwardLinks.size(); i++)
        {
          IssueLink link = (IssueLink) inwardLinks.get(i);
          String linkTypeId = link.getLinkTypeId().toString();
          log.debug("Examining link with type id " + linkTypeId);
          if ( inflictedLinkTypes.contains( linkTypeId ) ) {
            log.debug("Inflicted link found. issueLinkName: " + link.getIssueLinkType().getName());

            // the status of the other issue
            String issueStatus = link.getSourceObject().getStatusObject().getId();
            log.debug("Status on the other end of the link in " + issueStatus);

            if (  ! validStates.contains( issueStatus  ) ) {
              return false;
            }
          }
        }
        return true;
    }
}
