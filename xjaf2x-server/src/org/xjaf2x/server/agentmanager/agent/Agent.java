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

package org.xjaf2x.server.agentmanager.agent;

import java.io.Serializable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.SessionContext;
import javax.naming.NamingException;
import org.xjaf2x.server.JndiManager;
import org.xjaf2x.server.agentmanager.AgentManagerI;
import org.xjaf2x.server.messagemanager.MessageManagerI;
import org.xjaf2x.server.messagemanager.fipaacl.ACLMessage;

/**
 * Base class for all agents.
 * 
 * @author <a href="mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 * @author <a href="tntvteod@neobee.net">Teodor-Najdan Trifunov</a>
 */
@Lock(LockType.READ)
public abstract class Agent implements AgentI
{
	private static final long serialVersionUID = 1L;
	protected final Logger logger = Logger.getLogger(getClass().getName());
	protected AID myAid;
	protected AgentManagerI agm;
	protected MessageManagerI msm;
	private boolean processing;
	private BlockingQueue<ACLMessage> queue = new LinkedBlockingQueue<>();
	// TODO : replace with the managed executor service of Java EE 7
	private static final ExecutorService executor = Executors.newCachedThreadPool();
	@Resource
	private SessionContext context;
	
	@Override
	public void init(Serializable[] args)
	{
	}

	@Override
	public void terminate()
	{
	}

	/**
	 * Override this method to handle incoming messages.
	 * 
	 * @param msg
	 */
	protected abstract void onMessage(ACLMessage msg);

	@Override
	@Lock(LockType.WRITE)
	public final void handleMessage(ACLMessage msg)
	{
		queue.add(msg);
		if (!processing)
			processNextMessage();
	}

	@Override
	@Lock(LockType.WRITE)
	public final void processNextMessage()
	{
		final ACLMessage next = receive();
		if (next == null)
			processing = false;
		else
		{
			processing = true;
			final AgentI me = context.getBusinessObject(AgentI.class);
			executor.submit(new Runnable() {
				@Override
				public void run()
				{
					/*
					 * TODO : double-check if this holds. onMessage is not protected BUT (1) it is
					 * called only by processNextMessage (2) processNextMessage is called only by
					 * itself after executing onMessage, and by handleMessage (3) handleMessage will
					 * not call processNextMessage while onMessage is running, because of the
					 * "processing" flag
					 */
					onMessage(next);
					me.processNextMessage(); // will acquire lock
				}
			});
		}
	}

	/**
	 * Retrieves the next message from the queue, or null if the queue is empty. Equivalent to
	 * receive(-1).
	 * 
	 * @return ACLMessage object, or null if the queue is empty.
	 */
	protected ACLMessage receive()
	{
		return receive(-1);
	}

	/**
	 * Retrieves the next message from the queue, waiting up to the specified wait time if necessary
	 * for the message to become available.
	 * 
	 * @param timeout Maximum wait time, in milliseconds. If zero, the real time is not taken into
	 *            account and the method simply waits until a message is available. A negative
	 *            value will cause the method to return immediately, returning null if the queue was
	 *            empty.
	 * @return ACLMessage object, or null if the specified waiting time elapses before the message
	 *         is available.
	 */
	protected ACLMessage receive(long timeout)
	{
		ACLMessage msg = null;
		try
		{
			if (timeout < 0)
				timeout = 0;
			else if (timeout == 0)
				timeout = Long.MAX_VALUE;
			msg = queue.poll(timeout, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex)
		{
		}
		return msg;
	}

	@Override
	public int hashCode()
	{
		return myAid.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		return myAid.equals(((Agent)obj).myAid);
	}

	@Override
	public AID getAid()
	{
		return myAid;
	}
	
	@Override
	public String getNodeName()
	{
		return System.getProperty("jboss.node.name");
	}
	
	public final void setAid(AID aid) throws NamingException
	{
		this.myAid = aid;
		agm = JndiManager.getAgentManager();
		msm = JndiManager.getMessageManager();
	}
}
