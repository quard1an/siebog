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

package siebog.dnars.graph

import org.apache.commons.configuration.BaseConfiguration
import org.apache.commons.configuration.Configuration

import com.thinkaurelius.titan.core.TitanFactory
import com.thinkaurelius.titan.core.TitanGraph
import com.thinkaurelius.titan.core.util.TitanCleanup
import com.tinkerpop.blueprints.Direction
import com.tinkerpop.blueprints.Edge
import com.tinkerpop.blueprints.Graph
import com.tinkerpop.blueprints.Vertex
import com.tinkerpop.gremlin.scala.ScalaGraph
import com.tinkerpop.gremlin.scala.ScalaVertex

import siebog.dnars.base.Statement
import siebog.dnars.base.StatementParser
import siebog.dnars.base.Term
import siebog.dnars.base.Truth
import siebog.dnars.events.EventManager
import siebog.dnars.graph.DNarsVertex.wrap

/**
 * Wrapper around the ScalaGraph class. Inspired by
 * <a href="https://github.com/mpollmeier/gremlin-scala">gremlin/scala project</a>.
 *
 * @author <a href="mailto:mitrovic.dejan@gmail.com">Dejan Mitrovic</a>
 */
class DNarsGraph(override val graph: Graph, val domain: String) extends ScalaGraph(graph) {
	val statements = new StatementManager(this)
	val eventManager = new EventManager()

	def getV(term: Term): Option[Vertex] = {
		val i = this.query().has("term", term.id).limit(1).vertices().iterator()
		if (i.hasNext())
			Some(i.next())
		else
			None
	}

	/**
	 * Returns a vertex that corresponds to the given term.
	 * If the vertex does not exist, it will added to the graph.
	 */
	def getOrAddV(term: Term): Vertex = {
		getV(term) match {
			case Some(v) => v
			case None =>
				val added = addV(null)
				DNarsVertex(added).term = term
				added
		}
	}

	def addE(subj: Vertex, copula: String, pred: Vertex, truth: Truth): Edge = {
		val edge = subj.addEdge(copula, pred)
		DNarsEdge(edge).truth = truth
		edge
	}

	def getE(st: Statement): Option[Edge] = {
		val s = getV(st.subj)
		val p = getV(st.pred)
		if (s == None || p == None) // no vertex, so no edge
			None
		else {
			// vertices exist, check for an edge
			val subj: ScalaVertex = s.get
			val list = subj.outE(st.copula).as("x").inV.retain(Seq(p.get)).back("x").toList
			list match {
				case List() => None // nope
				case h :: Nil => Some(h.asInstanceOf[Edge])
				case _ => throw new IllegalStateException(s"Multiple edges of the same copula for $st")
			}
		}
	}

	/**
	 * Debugging purposes only.
	 */
	def printEdges(): Unit = {
		println(s"---------------- Graph dump [domain=$domain] ----------------")
		forEachStatement(println(_))
		println("------------------- Done -------------------")
	}

	def forEachStatement(f: (Statement) => Unit): Unit = {
		val allSt = E.map { e =>
			val s: DNarsVertex = e.getVertex(Direction.OUT)
			val p: DNarsVertex = e.getVertex(Direction.IN)
			val c = e.getLabel
			val t = DNarsEdge(e).truth
			val st = Statement(s.term, c, p.term, t)
			// print only the packed version
			st.pack() match {
				case List() => st
				case List(h, _) => h
			}
		}.toSet
		allSt.foreach(f)
	}

	/**
	 * Debugging purposes only.
	 */
	def getEdgeCount(): Long = {
		var count = 0L
		V.as("x").inE.sideEffect { e => count += 1 }.back("x").outE.sideEffect { e => count += 1 }.iterate
		count
	}

	def shutdown(): Unit = graph.shutdown()

	def clear(): Unit = graph match {
		case tg: TitanGraph =>
			TitanCleanup.clear(tg)
		case any: Any =>
			throw new IllegalArgumentException(any.getClass.getName + " cannot be cleared")
	}

	def addObserver(aid: String): Unit =
		eventManager.addObserver(aid)

	def removeObserver(aid: String): Unit =
		eventManager.removeObserver(aid)

	def addStatement(st: String): Unit =
		try {
			statements.add(StatementParser(st))
		} catch {
			case e: Throwable =>
				throw new IllegalArgumentException(e.getMessage)
		}

	/**
	 * Debugging purposes only.
	 */
	def getRandomVertex(): Vertex =
		this.V.random(0.5).next
}

object DNarsGraph {
	def apply(graph: ScalaGraph, keyspace: String) = wrap(graph, keyspace)
	implicit def wrap(graph: ScalaGraph, keyspace: String) = new DNarsGraph(graph, keyspace)
	implicit def unwrap(wrapper: DNarsGraph) = wrapper.graph
}

object DNarsGraphFactory {
	def create(domain: String, additionalConfig: java.util.Map[String, Any] = null): DNarsGraph = {
		val conf = getConfig(domain, additionalConfig)
		val graph = TitanFactory.open(conf)
		try {
			graph.makeKey("term").dataType(classOf[String]).indexed(classOf[Vertex]).unique().make()
		} catch {
			case _: IllegalArgumentException => // index already exists, ok
			case e: Throwable => throw e
		}
		DNarsGraph(ScalaGraph(graph), domain)
	}

	private def getConfig(domain: String, additionalConfig: java.util.Map[String, Any]): Configuration = {
		val conf = new BaseConfiguration
		conf.setProperty("storage.backend", "cassandra")
		conf.setProperty("storage.hostname", "localhost");
		conf.setProperty("storage.keyspace", domain)
		// additional configuration?
		if (additionalConfig != null) {
			val es = additionalConfig.entrySet()
			val i = es.iterator()
			while (i.hasNext()) {
				val c = i.next()
				conf.setProperty(c.getKey(), c.getValue())
			}
		}
		// done
		conf
	}
}