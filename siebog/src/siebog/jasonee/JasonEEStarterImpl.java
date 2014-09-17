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

package siebog.jasonee;

import jason.mas2j.AgentParameters;
import jason.mas2j.ClassParameters;
import jason.mas2j.MAS2JProject;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import siebog.core.Global;
import siebog.jasonee.control.ExecutionControl;
import siebog.jasonee.control.UserExecutionControl;
import siebog.jasonee.intf.JasonEEEnvironment;
import siebog.jasonee.intf.JasonEEStarter;
import siebog.utils.ObjectFactory;
import siebog.xjaf.core.AgentClass;
import siebog.xjaf.managers.AgentInitArgs;
import siebog.xjaf.managers.AgentManager;

/**
 * 
 * @author <a href="mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 */
@Stateless
@Remote(JasonEEStarter.class)
@LocalBean
@Path("/jasonee")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JasonEEStarterImpl implements JasonEEStarter {
	private static final Logger logger = Logger.getLogger(JasonEEStarterImpl.class.getName());
	private MAS2JProject project;
	private String remObjFactModule;
	private String remObjFactEjb;
	private RemoteObjectFactory remObjFact;
	private String ctrlName;
	private String envName;

	@POST
	@Path("/")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	@Override
	public void start(@FormParam("mas2jFileName") String mas2jFileName) {
		project = Mas2jProjectFactory.load(new File(mas2jFileName));
		createRemObjFact();

		createExecutionControl();
		createEnvironment();

		createAgents(mas2jFileName);
	}

	private void createRemObjFact() {
		final List<AgentParameters> agents = project.getAgents();
		for (AgentParameters agp : agents)
			if (agp.name.equals(RemoteObjectFactory.NAME)) {
				remObjFactModule = agp.getOption("module");
				remObjFactEjb = agp.getOption("object");
				remObjFact = ObjectFactory.getRemoteObjectFactory(remObjFactModule, remObjFactEjb);
				return;
			}
		throw new IllegalArgumentException("Need to specify the RemoteObjectFactory object.");
	}

	private void createExecutionControl() {
		ExecutionControl ctrl = ObjectFactory.getExecutionControl();
		UserExecutionControl userExecCtrl = null;
		ClassParameters userClass = project.getControlClass();
		if (userClass != null) {
			final String userClassName = userClass.getClassName();
			try {
				userExecCtrl = remObjFact.createExecutionControl(userClassName);
				userExecCtrl.init(project.getControlClass().getParametersArray());
			} catch (Exception ex) {
				logger.log(Level.WARNING, "Unable to create user execution control " + userClassName, ex);
			}
		}
		ctrl.init(userExecCtrl);
		ctrlName = ObjectFactory.getJasonEEApp().putExecCtrl(ctrl);
	}

	private void createEnvironment() {
		JasonEEEnvironment env = ObjectFactory.getJasonEEEnvironment();
		final ClassParameters userEnvClass = project.getEnvClass();
		env.init(userEnvClass.getClassName(), userEnvClass.getParametersArray());
		envName = ObjectFactory.getJasonEEApp().putEnv(env);
	}

	private void createAgents(String mas2jFileName) {
		AgentManager agm = ObjectFactory.getAgentManager();
		final List<AgentParameters> agents = project.getAgents();
		for (AgentParameters agp : agents) {
			String runtimeName = agp.name;
			if (runtimeName.equals(RemoteObjectFactory.NAME))
				continue;
			for (int i = 0; i < agp.qty; i++) {
				if (agp.qty > 1)
					runtimeName += (i + 1);
				AgentClass agClass = new AgentClass(Global.SERVER, JasonEEAgent.class.getSimpleName());
				AgentInitArgs args = new AgentInitArgs();
				args.put("agentName", agp.name);
				args.put("mas2jFileName", mas2jFileName);
				args.put("remObjFactModule", remObjFactModule);
				args.put("remObjFactEjb", remObjFactEjb);
				args.put("envName", envName);
				args.put("execCtrlName", ctrlName);
				agm.startAgent(agClass, runtimeName, args);
			}
		}
	}
}
