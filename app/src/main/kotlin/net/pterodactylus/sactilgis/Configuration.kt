package net.pterodactylus.sactilgis

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter

class Configuration {

	class Subversion {

		var url: String = ""

	}

	class Branch {

		var name: String = ""

		@JsonProperty("revision-paths")
		val revisionPaths: List<RevisionPath> = ArrayList()

		class RevisionConverter : StdConverter<String?, Long?>() {

			override fun convert(value: String?): Long {
				if (value == "HEAD") {
					return -1L
				}
				return value!!.toLong()
			}
		}

		class RevisionPath {

			@JsonDeserialize(converter = RevisionConverter::class)
			var revision: Long = 0
			var path: String = ""

		}

	}

	val subversion: Subversion = Subversion()
	val branches: List<Branch> = ArrayList()

}
