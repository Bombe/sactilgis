package net.pterodactylus.utils.svn

import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.util.SortedSet
import java.util.TreeMap
import java.util.TreeSet

class RepositoryScanner(svnUrl: SVNURL) {

	fun addBranch(name: String, vararg pathFirstRevisions: Pair<Long, String>) {
		branchDefinitions.getOrPut(name) { TreeMap() }.putAll(pathFirstRevisions)
	}

	fun identifyBranches(): Map<String, SortedSet<Long>> {
		val branchRevisions = mutableMapOf<String, TreeSet<Long>>()
		val latestRevision = svnRepository.latestRevision

		LongRange(1, latestRevision).forEach { revision ->
			simpleSvn.getLogEntry("/", revision)!!
				.changedPaths.keys
				.mapNotNull { path -> findBranchByPathAndRevision(path, revision) }
				.distinct()
				.forEach { branch -> branchRevisions.getOrPut(branch) { TreeSet() }.add(revision) }
		}

		return branchRevisions
	}

	private fun findBranchByPathAndRevision(path: String, revision: Long): String? =
		branchDefinitions
			.mapValues { (_, revisionPaths) -> revisionPaths.floorEntry(revision)?.value }
			.filter { it.value != null }
			.filterValues { p -> path.startsWith(p!!) }
			.keys.singleOrNull()

	private val simpleSvn = SimpleSVN(svnUrl)
	private val svnRepository = SVNRepositoryFactory.create(svnUrl)

	private val branchDefinitions = mutableMapOf<String, TreeMap<Long, String>>()

}

private operator fun Pair<Long, Long>.contains(value: Long) =
	if (second != -1L) {
		value in first..second
	} else {
		value >= first
	}
