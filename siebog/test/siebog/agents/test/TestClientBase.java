/**
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements. See the NOTICE file 
 * distributed with this work for additional information regarding 
 * copyright ownership. The ASF licenses this file to you under 
 * the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may 
 * obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, 
 * software distributed under the License is distributed on an 
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 
 * either express or implied. 
 * 
 * See the License for the specific language governing permissions 
 * and limitations under the License.
 */

package siebog.agents.test;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import siebog.SiebogClient;
import siebog.agents.AID;
import siebog.agents.AgentManager;
import siebog.interaction.ACLMessage;
import siebog.interaction.MessageManager;
import siebog.utils.ObjectFactory;

/**
 * Base class for all test client applications.
 * 
 * @author <a href="mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 */
public abstract class TestClientBase {
	protected Logger logger;
	protected BlockingQueue<ACLMessage> msgQueue;
	protected AID testAgentAid;
	protected AgentManager agm;
	protected MessageManager msm;

	public TestClientBase() throws RemoteException {
		TestProps props = TestProps.get();
		SiebogClient.connect(props.getMaster(), props.getSlaves());
		logger = Logger.getLogger(getClass().getName());
		msgQueue = new LinkedBlockingQueue<>();
		agm = ObjectFactory.getAgentManager();
		msm = ObjectFactory.getMessageManager();
		startTestAgent(props.getMaster());
	}

	protected ACLMessage pollMessage() {
		try {
			return msgQueue.poll(5, TimeUnit.SECONDS);
		} catch (InterruptedException ex) {
			return null;
		}
	}

	private void startTestAgent(String masterAddr) throws RemoteException {
		Registry reg;
		try {
			reg = LocateRegistry.createRegistry(1099);
		} catch (Exception ex) {
			reg = LocateRegistry.getRegistry(1099);
		}
		// reg.rebind(RemoteAgentListener.class.getSimpleName(), new
		// RemoteAgentListenerImpl(msgQueue));
		// AgentClass agClass = new AgentClass(Global.SIEBOG_MODULE,
		// RemoteAgent.class.getSimpleName());
		// AgentInitArgs args = new AgentInitArgs("remoteHost=" + masterAddr);
		// testAgentAid = agm.startServerAgent(agClass, "testAgent", args);
	}
}
