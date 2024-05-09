package net.pterodactylus.sactilgis

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import net.pterodactylus.sactilgis.Configuration.Branch.Merge
import net.pterodactylus.sactilgis.Configuration.Branch.Origin
import net.pterodactylus.sactilgis.Configuration.Branch.Tag

data class Configuration(
	val general: General = General(),
	val branches: MutableList<Branch> = mutableListOf(),
	val committers: MutableList<Committer> = mutableListOf()
) {

	data class General(
		@JsonProperty("subversion-url")
		var subversionUrl: String? = null,
		@JsonProperty("subversion-auth")
		var subversionAuth: SubversionAuth? = null,
		@JsonProperty("committer")
		var committer: Committer? = null,
		@JsonProperty("target-directory")
		var targetDirectory: String? = null,
		@JsonProperty("use-commit-date-from-entry")
		var useCommitDateFromEntry: Boolean? = null,
		@JsonProperty("ignore-global-gitignore-file")
		var ignoreGlobalGitIgnoreFile: Boolean? = null,
		@JsonProperty("sign-commits")
		var signCommits: Boolean? = null
	)

	data class SubversionAuth(
		@JsonProperty("username")
		var username: String? = null,
		@JsonProperty("password")
		var password: String? = null
	)

	class Branch {

		var name: String = ""
		var origin: Origin? = null
		@JsonProperty("revision-paths")
		val revisionPaths: MutableList<RevisionPath> = mutableListOf()
		val merges: MutableList<Merge> = mutableListOf()
		val tags: MutableList<Tag> = mutableListOf()
		val fixes: MutableList<Fix> = mutableListOf()

		data class Origin(
			@JsonProperty("tag")
			var tag: String? = null,
			@JsonProperty("branch")
			var branch: String? = null,
			@JsonProperty("revision")
			var revision: Long? = null,
		)

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

		class Merge {

			var revision: Long = 0
			var branch: String? = null
			var tag: String? = null

		}

		class Tag {

			var revision: Long = 0
			var name: String = ""
			@JsonProperty("message-revision")
			var messageRevision: Long = 0

		}

		class Fix {

			var revision: Long = 0
			var message: String = ""

		}

	}

	data class Committer(
		@get:JsonProperty("id")
		var subversionId: String = "",
		var name: String = "",
		var email: String = ""
	)

	fun merge(configuration: Configuration): Configuration {
		val mergedConfiguration = Configuration()
		mergedConfiguration.general.subversionUrl = configuration.general.subversionUrl ?: general.subversionUrl
		mergedConfiguration.general.subversionAuth = configuration.general.subversionAuth ?: general.subversionAuth
		mergedConfiguration.general.committer = configuration.general.committer ?: general.committer
		mergedConfiguration.general.targetDirectory = configuration.general.targetDirectory ?: general.targetDirectory
		mergedConfiguration.general.useCommitDateFromEntry = configuration.general.useCommitDateFromEntry ?: general.useCommitDateFromEntry
		mergedConfiguration.general.ignoreGlobalGitIgnoreFile = configuration.general.ignoreGlobalGitIgnoreFile ?: general.ignoreGlobalGitIgnoreFile
		mergedConfiguration.general.signCommits = configuration.general.signCommits ?: general.signCommits
		mergedConfiguration.committers += committers.filterNot { it.subversionId in configuration.committers.map(Committer::subversionId) } + configuration.committers
		mergedConfiguration.branches += configuration.branches.takeIf(List<*>::isNotEmpty) ?: branches
		return mergedConfiguration
	}

	fun verify() {
		branches.filter { " " in it.name }.onNotEmpty { throw IllegalStateException("Invalid branch names: $it") }
		branches.filter { it.revisionPaths.isEmpty() }.onNotEmpty { throw IllegalStateException("Empty revision paths: $it") }

		val allDefinedBranches = branches.map(Branch::name)
		if (allDefinedBranches.size != allDefinedBranches.distinct().size) {
			throw IllegalStateException("Duplicate branches found: ${allDefinedBranches.groupBy { it }.filterValues { it.size > 1 }.keys}")
		}

		val allDefinedTags = branches.flatMap { it.tags }.map(Tag::name)
		if (allDefinedTags.size != allDefinedTags.distinct().size) {
			throw IllegalStateException("Duplicate tags found: ${allDefinedTags.groupBy { it }.filterValues { it.size > 1 }.keys}")
		}
		if (allDefinedTags.any { " " in it }) {
			throw IllegalStateException("Invalid tag found: ${allDefinedTags.filter { " " in it }}")
		}

		val allOriginTags = branches.mapNotNull(Branch::origin).mapNotNull(Origin::tag)
		if (allOriginTags.any { " " in it }) {
			throw IllegalStateException("Invalid origin tag found: ${allOriginTags.filter { " " in it }}")
		}
		if (!allDefinedTags.containsAll(allOriginTags)) {
			throw IllegalStateException("Missing origin tag found: ${allOriginTags - allDefinedTags}")
		}

		val allOriginBranches = branches.mapNotNull(Branch::origin).mapNotNull(Origin::branch)
		if (allOriginBranches.any { it !in allDefinedBranches }) {
			throw IllegalStateException("Missing origin branch found: ${allOriginBranches.filter { it !in allDefinedBranches }}")
		}

		val allMerges = branches.flatMap(Branch::merges)
		if (allMerges.any { (it.branch != null) && (it.branch !in allDefinedBranches) }) {
			throw IllegalStateException("Missing branch to merge found: ${allMerges.filter { (it.branch != null) && (it.branch !in allDefinedBranches) }.map(Merge::branch)}")
		}
		if (allMerges.any { (it.tag != null) && (it.tag !in allDefinedTags) }) {
			throw IllegalStateException("Missing tag to merge found: ${allMerges.filter { (it.tag != null) && (it.tag !in allDefinedTags) }.map(Merge::tag)}")
		}
	}

}

private fun <T> Collection<T>.onNotEmpty(action: (Collection<T>) -> Unit) = run {
	if (isNotEmpty()) {
		action(this)
	}
}
