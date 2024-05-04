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

	fun identifyBranches(): RepositoryInformation {
		val branchRevisions = mutableMapOf<String, TreeSet<Long>>()
		val branchCreationPoints = mutableMapOf<String, Pair<String, Long>>()
		val latestRevision = svnRepository.latestRevision

		LongRange(1, latestRevision).forEach { revision ->
			val logEntry = simpleSvn.getLogEntry("/", revision)!!
			logEntry
				.changedPaths.keys
				.mapNotNull { path -> findBranchByPathAndRevision(path, revision) }
				.distinct()
				.forEach { branch ->
					if (branch !in branchRevisions) {
						val path = findPathForBranchAtRevision(branch, revision)
						logEntry.changedPaths[path]?.let { branchPath ->
							if (branchPath.copyRevision != -1L) {
								val sourceBranch = findBranchByPathAndRevision(branchPath.copyPath, branchPath.copyRevision)!!
								val lastRevisionOnSourceBranch = branchRevisions[sourceBranch]!!.floor(branchPath.copyRevision)!!
								branchCreationPoints[branch] = branchPath.copyPath to lastRevisionOnSourceBranch
							}
						}
					}
					branchRevisions.getOrPut(branch) { TreeSet() }.add(revision)
				}
		}

		return RepositoryInformation(latestRevision, branchRevisions, branchCreationPoints)
	}

	private fun findBranchByPathAndRevision(path: String, revision: Long): String? =
		branchDefinitions
			.mapValues { (_, revisionPaths) -> revisionPaths.floorEntry(revision)?.value }
			.filter { it.value != null }
			.filterValues { p -> path.startsWith(p!!) }
			.keys.singleOrNull()

	private fun findPathForBranchAtRevision(branch: String, revision: Long): String? =
		branchDefinitions[branch]
			?.floorEntry(revision)
			?.value

	private val simpleSvn = SimpleSVN(svnUrl)
	private val svnRepository = SVNRepositoryFactory.create(svnUrl)

	private val branchDefinitions = mutableMapOf<String, TreeMap<Long, String>>()

}

data class RepositoryInformation(val latestRevision: Long, val brachRevisions: Map<String, SortedSet<Long>>, val branchCreationPoints: Map<String, Pair<String, Long>>)

private operator fun Pair<Long, Long>.contains(value: Long) =
	if (second != -1L) {
		value in first..second
	} else {
		value >= first
	}
