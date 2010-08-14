/*
 * Copyright (c) 2002-2004 Alex Perez <quimicefa@gmail.com>
 * Copyright (c) 2010 Dmitry Astapov <dastapov@gmail.com>
 * All rights reserved.
 */
package ua.astapov.jira.plugins.blockinglinks;

import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.comparator.StatusComparator;
import com.atlassian.jira.issue.link.IssueLinkTypeManager;
import com.atlassian.jira.issue.link.IssueLinkType;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.plugin.workflow.AbstractWorkflowPluginFactory;
import com.atlassian.jira.plugin.workflow.WorkflowPluginConditionFactory;
import com.opensymphony.workflow.loader.AbstractDescriptor;
import com.opensymphony.workflow.loader.ConditionDescriptor;
import org.ofbiz.core.entity.GenericValue;
import org.ofbiz.core.util.UtilMisc;
import org.apache.log4j.Logger;

import java.util.*;

public class WorkflowBlockingLinksConditionFactoryImpl extends AbstractWorkflowPluginFactory implements WorkflowPluginConditionFactory
{
    private final ConstantsManager constantsManager;
    private final IssueLinkTypeManager issueLinkTypeManager;
    private static final Logger log = Logger.getLogger(WorkflowBlockingLinksConditionFactoryImpl.class);

    public WorkflowBlockingLinksConditionFactoryImpl(ConstantsManager constantsManager, IssueLinkTypeManager issueLinkTypeManager)
    {
        this.constantsManager = constantsManager;
        this.issueLinkTypeManager = issueLinkTypeManager;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	protected void getVelocityParamsForInput(Map velocityParams)
    {
        Collection<Status> statuses = constantsManager.getStatusObjects();
        log.debug("statuses in getVelocityParamsForInput:" + statuses.toString());
        velocityParams.put("statuses", Collections.unmodifiableCollection(statuses));

        Collection<IssueLinkType> linkTypes = issueLinkTypeManager.getIssueLinkTypes();
        log.debug("linkTypes in getVelocityParamsForInput:" + linkTypes.toString());
        velocityParams.put("linkTypes", Collections.unmodifiableCollection(linkTypes));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	protected void getVelocityParamsForEdit(Map velocityParams, AbstractDescriptor descriptor)
    {
        getVelocityParamsForInput(velocityParams);
        velocityParams.put("selectedStatuses", getSelected("statuses",descriptor));
        velocityParams.put("selectedLinkTypes", getSelected("linkTypes",descriptor));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	protected void getVelocityParamsForView(Map velocityParams, AbstractDescriptor descriptor)
    {
      Collection<String> selectedStatusIds = getSelected("statuses",descriptor);
      Collection<String> selectedLinkTypeIds = getSelected("linkTypes",descriptor);
      log.debug("selectedStatusIds in getVelocityParamsForView:" + selectedStatusIds.toString());
      log.debug("selectedLinkTypeIds in getVelocityParamsForView:" + selectedLinkTypeIds.toString());

      List<GenericValue> selectedStatuses = new LinkedList<GenericValue>();
      for (Iterator<String> iterator = selectedStatusIds.iterator(); iterator.hasNext();)
      {
        String statusId = iterator.next();
        selectedStatuses.add(constantsManager.getStatusObject(statusId).getGenericValue());
      }

      List<IssueLinkType> selectedLinkTypes = new LinkedList<IssueLinkType>();
      for (Iterator<String> iterator = selectedLinkTypeIds.iterator(); iterator.hasNext();)
      {
        String linkTypeId = (String) iterator.next();
        log.debug("linkTypeId to get is " + linkTypeId);
        selectedLinkTypes.add(issueLinkTypeManager.getIssueLinkType(new Long(linkTypeId)));
      }

      // Sort the list of statuses so as they are displayed consistently
      Collections.sort(selectedStatuses, new StatusComparator());
      Collections.sort(selectedLinkTypes);

      velocityParams.put("statuses", Collections.unmodifiableCollection(selectedStatuses));
      velocityParams.put("linkTypes", Collections.unmodifiableCollection(selectedLinkTypes));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Map getDescriptorParams(Map conditionParams)
    {
      log.debug("conditionParams in getDescriptorParams:" + conditionParams.toString());

      String[] issue_statuses = (String []) conditionParams.get("issue_statuses");
      String[] link_types = (String []) conditionParams.get("link_types");

      Map m = UtilMisc.toMap("statuses", arrayToCommaSep(issue_statuses));
      m.put("linkTypes", arrayToCommaSep(link_types));
      return m;
    }

  // arr is guaranteed to be non-empty by the JavaScript in the UI
  private String arrayToCommaSep(String[] arr) {
    StringBuffer out = new StringBuffer();

    for (int i = 0; i < arr.length; i++) {
      out.append(arr[i] + ",");
    }

    return out.substring(0, out.length() - 1);
  }

    private Collection<String> getSelected(String things, AbstractDescriptor descriptor)
    {
        Collection<String> selectedThings = new LinkedList<String>();
        if (!(descriptor instanceof ConditionDescriptor))
        {
            throw new IllegalArgumentException("Descriptor must be a ConditionDescriptor.");
        }

        ConditionDescriptor conditionDescriptor = (ConditionDescriptor) descriptor;

        String allThings = (String) conditionDescriptor.getArgs().get(things);
        StringTokenizer st = new StringTokenizer(allThings, ",");

        while (st.hasMoreTokens())
        {
            selectedThings.add(st.nextToken());
        }

        return selectedThings;
    }
}
