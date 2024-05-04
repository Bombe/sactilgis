package net.pterodactylus.utils.svn

import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNRepositoryFactory

class RepositoryScanner(svnUrl: SVNURL) {

	fun addBranch(name: String, vararg pathRanges: Pair<String, Pair<Long, Long>>) {
		branchDefinitions.merge(name, pathRanges.toList()) { l1, l2 -> l1 + l2 }
	}

	fun identifyBranches(): Map<String, List<Long>> {
		val branchRevisions = mutableMapOf<String, MutableList<Long>>()
		val latestRevision = svnRepository.latestRevision

		LongRange(1, latestRevision).forEach { revision ->
			val branch = simpleSvn.getLogEntry("/", revision)!!
				.changedPaths.keys
				.mapNotNull { path -> findBranchByPathAndRevision(path, revision) }
				.distinct()
				.single()
			branchRevisions.getOrPut(branch) { mutableListOf() }.add(revision)
		}

		return branchRevisions
	}

	private fun findBranchByPathAndRevision(path: String, revision: Long): String? =
		branchDefinitions
			.mapValues { (_, pathRanges) -> pathRanges.singleOrNull { (_, range) -> revision in range } }
			.filter { it.value != null }
			.filterValues { pathRanges -> path.startsWith(pathRanges!!.first) }
			.keys.singleOrNull()

	private val simpleSvn = SimpleSVN(svnUrl)
	private val svnRepository = SVNRepositoryFactory.create(svnUrl)

	private val branchDefinitions = mutableMapOf<String, Collection<Pair<String, Pair<Long, Long>>>>()

}

private operator fun Pair<Long, Long>.contains(value: Long) =
	if (second != -1L) {
		value in first..second
	} else {
		value >= first
	}
