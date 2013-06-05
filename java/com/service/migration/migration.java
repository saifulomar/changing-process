package com.service.migration;

import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.process.WorkItem;
import org.drools.runtime.process.WorkItemHandler;
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;

public class migration implements WorkItemHandler{
	
	private StatefulKnowledgeSession ksession;


	public migration(StatefulKnowledgeSession ksession) {
		this.ksession = ksession;	
	}


	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		try {
			String sinstanceId = (String) workItem.getParameter("instanceId");
			long instanceId = Long.parseLong(sinstanceId);
			String processId = (String) workItem.getParameter("processId");

            WorkflowProcessInstanceUpgrader.upgradeProcessInstance(ksession, instanceId, processId, null);
			manager.completeWorkItem(workItem.getId(), null);
		} catch (Throwable t) {
			manager.abortWorkItem(workItem.getId());
		}
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		// Do nothing
	}

}
