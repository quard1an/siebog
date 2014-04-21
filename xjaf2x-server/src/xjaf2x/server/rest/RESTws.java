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

package xjaf2x.server.rest;

import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import java.util.List;

import xjaf2x.server.Global;
/**
 *
 * @author <a href="rade.milovanovic@hotmail.com">Rade Milovanovic</a>
 */

@Path("/rest")
public class RESTws {

	@GET
	@Produces("application/json")
	@Path("/getfamilies")
	public String getFamilies()
	{
		
		StringBuilder lista = new StringBuilder();
		lista.append("\"families\":[");
		List<String> families;		
		try {
			families =  Global.getAgentManager().getFamilies();
			for (String str : families)
				lista.append("\"" + str + "\", ");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		int i = lista.lastIndexOf(",");
		int k = lista.length();
		lista.replace(i,k, " ]");
		return lista.toString();
	}
	
}
