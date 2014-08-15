package siebog.server.xjaf.agents.base;

import java.io.Serializable;

public class AgentClass implements Serializable
{
	private static final long serialVersionUID = 1L;
	private final String module;
	private final String ejbName;

	public AgentClass(String module, String ejbName)
	{
		this.module = module;
		this.ejbName = ejbName;
	}

	public String getModule()
	{
		return module;
	}

	public String getEjbName()
	{
		return ejbName;
	}
	
	public static AgentClass valueOf(String str)
	{
		int n = str.indexOf('-');
		return new AgentClass(str.substring(0, n), str.substring(n + 1));
	}
}
