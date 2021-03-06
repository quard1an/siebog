package dnars.utils.importers.nt

import java.io.PrintWriter
import com.hp.hpl.jena.rdf.model.RDFNode

object RDF2Narsese {
	def main(args: Array[String]): Unit = {
		if (args.length != 2) {
			println("I need 2 arguments: InputFile OutputFile")
			return
		}
		val input = args(0)
		val output = args(1)

		println(s"Reading from $input...")
		val out = new PrintWriter(output)
		try {
			val total = NTReader.read(input, (line, ntStat, counter) => {
				val st = DNarsConvert.toDNarsStatement(ntStat)
				val pattern = "[#\\[\\]%{}\\-*|,<>]"
				val subj = st.subj.id.replaceAll(pattern, "_").replace("(x", "<(*").replace(' ', ',')
				val pred = st.pred.id.replaceAll(pattern, "_")
				val str = s"${subj} --> ${pred}>. %1.0;0.9%"
				out.println(str)
				if (counter % 10000 == 0)
					println(s"Converted $counter statements...")
				true
			})

			println(s"Done. Total: $total statements.")
		} finally {
			out.close()
		}
	}

	private def getStr(node: RDFNode): String =
		node.toString
			.trim
			.replaceAll("""\s""", "_")
			.replace("%", "$")
}