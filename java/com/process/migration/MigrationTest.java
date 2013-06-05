package com.process.migration;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import org.drools.KnowledgeBase;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderConfiguration;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.command.impl.GenericCommand;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.event.process.ProcessCompletedEvent;
import org.drools.event.process.ProcessEventListener;
import org.drools.event.process.ProcessNodeLeftEvent;
import org.drools.event.process.ProcessNodeTriggeredEvent;
import org.drools.event.process.ProcessStartedEvent;
import org.drools.event.process.ProcessVariableChangedEvent;
import org.drools.io.ResourceFactory;
import org.drools.marshalling.ObjectMarshallingStrategy.Context;
import org.drools.runtime.Environment;
import org.drools.runtime.StatefulKnowledgeSession;
import org.jbpm.bpmn2.xml.BPMNDISemanticModule;
import org.jbpm.bpmn2.xml.BPMNSemanticModule;
import org.jbpm.compiler.xml.XmlProcessReader;
import org.jbpm.compiler.xml.XmlRuleFlowProcessDumper;
import org.jbpm.process.instance.impl.ProcessInstanceImpl;
import org.jbpm.ruleflow.core.RuleFlowProcess;
import org.jbpm.test.JbpmJUnitTestCase.TestWorkItemHandler;
import org.jbpm.workflow.core.Connection;
import org.jbpm.workflow.core.impl.ConnectionImpl;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.core.node.HumanTaskNode;
import org.junit.Test;

/**
 * This is a sample file to launch a process.
 */
public class MigrationTest extends JbpmBpmn2TestCase {

    private HashMap<String, Object> context;
    private Environment env;
    

    @Test
	public void testDynamicProcess() throws Exception {	

		KnowledgeBuilderConfiguration conf = KnowledgeBuilderFactory
        .newKnowledgeBuilderConfiguration();
		((PackageBuilderConfiguration) conf).initSemanticModules();
		((PackageBuilderConfiguration) conf)
		        .addSemanticModule(new BPMNSemanticModule());
		((PackageBuilderConfiguration) conf)
		        .addSemanticModule(new BPMNDISemanticModule());
		XmlProcessReader processReader = new XmlProcessReader(
		        ((PackageBuilderConfiguration) conf).getSemanticModules(),
		        getClass().getClassLoader());
		List<org.drools.definition.process.Process> processes = processReader
		        .read(MigrationTest.class
		                .getResourceAsStream("/BPMN2-UserTask.bpmn2"));

		final RuleFlowProcess process = (RuleFlowProcess) processes.get(0);
		
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
		        .newKnowledgeBuilder(conf);
		kbuilder.add(ResourceFactory.newReaderResource(new StringReader(
				XmlRuleFlowProcessDumper.INSTANCE.dump(process))), ResourceType.DRF);
		KnowledgeBase kbase = kbuilder.newKnowledgeBase();
		StatefulKnowledgeSession ksession = createKnowledgeSession(kbase);
		TestWorkItemHandler testHandler = new TestWorkItemHandler();
		ksession.getWorkItemManager().registerWorkItemHandler("Human Task", testHandler);
		ksession.addEventListener(new ProcessEventListener() {
			public void beforeVariableChanged(ProcessVariableChangedEvent arg0) {
			}
			public void beforeProcessStarted(ProcessStartedEvent arg0) {
				System.out.println(arg0);
			}
			public void beforeProcessCompleted(ProcessCompletedEvent arg0) {
				System.out.println(arg0);
			}
			public void beforeNodeTriggered(ProcessNodeTriggeredEvent arg0) {
				System.out.println(arg0);
			}
			public void beforeNodeLeft(ProcessNodeLeftEvent arg0) {
				System.out.println(arg0);
			}
			public void afterVariableChanged(ProcessVariableChangedEvent arg0) {
			}
			public void afterProcessStarted(ProcessStartedEvent arg0) {
			}
			public void afterProcessCompleted(ProcessCompletedEvent arg0) {
			}
			public void afterNodeTriggered(ProcessNodeTriggeredEvent arg0) {
			}
			public void afterNodeLeft(ProcessNodeLeftEvent arg0) {
			}
		});

		final ProcessInstanceImpl processInstance = (ProcessInstanceImpl)
			ksession.startProcess("UserTask");
		
		HumanTaskNode node = new HumanTaskNode();
		node.setName("Task2");
		node.setId(4);
		insertNodeInBetween(process, 2, 3, node);
		
		((CommandBasedStatefulKnowledgeSession) ksession).getCommandService().execute(new GenericCommand<Void>() {
			public Void execute(Context context) {
				StatefulKnowledgeSession ksession = (StatefulKnowledgeSession) ((KnowledgeCommandContext) context).getKieSession();
				((ProcessInstanceImpl) ksession.getProcessInstance(processInstance.getId())).updateProcess(process);
				return null;
			}
		});
		
		ksession.getWorkItemManager().completeWorkItem(testHandler.getWorkItem().getId(), null);
		
		ksession.getWorkItemManager().completeWorkItem(testHandler.getWorkItem().getId(), null);
	}

	private static void insertNodeInBetween(RuleFlowProcess process, long startNodeId, long endNodeId, NodeImpl node) {
		if (process == null) {
			throw new IllegalArgumentException("Process may not be null");
		}
		NodeImpl selectedNode = (NodeImpl) process.getNode(startNodeId);
		if (selectedNode == null) {
			throw new IllegalArgumentException("Node " + startNodeId + " not found in process " + process.getId());
		}
		for (Connection connection: selectedNode.getDefaultOutgoingConnections()) {
			if (connection.getTo().getId() == endNodeId) {
				process.addNode(node);
				NodeImpl endNode = (NodeImpl) connection.getTo();
				((ConnectionImpl) connection).terminate();
				new ConnectionImpl(selectedNode, NodeImpl.CONNECTION_DEFAULT_TYPE, node, NodeImpl.CONNECTION_DEFAULT_TYPE);
				new ConnectionImpl(node, NodeImpl.CONNECTION_DEFAULT_TYPE, endNode, NodeImpl.CONNECTION_DEFAULT_TYPE);
				return;
			}
		}
		throw new IllegalArgumentException("Connection to node " + endNodeId + " not found in process " + process.getId());
	}
	
}