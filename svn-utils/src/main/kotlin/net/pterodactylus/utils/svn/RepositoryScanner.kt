package net.pterodactylus.utils.svn

import org.tmatesoft.svn.core.io.SVNRepository
import java.util.SortedSet
import java.util.TreeSet

class RepositoryScanner(private val svnRepository: SVNRepository) {

	fun addBranch(name: String, branchDefinition: BranchDefinition) {
		branchDefinitions[name] = branchDefinition
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
					val path = findPathForBranchAtRevision(branch, revision)
					if (branch !in branchRevisions) {
						logEntry.changedPaths[path]?.let { branchPath ->
							if (branchPath.copyRevision != -1L) {
								val sourceBranch = findBranchByPathAndRevision(branchPath.copyPath, branchPath.copyRevision)!!
								val lastRevisionOnSourceBranch = branchRevisions[sourceBranch]!!.floor(branchPath.copyRevision)!!
								branchCreationPoints[branch] = branchPath.copyPath to lastRevisionOnSourceBranch
							}
						}
					}
					if (logEntry.changedPaths[path]?.type != 'D') {
						branchRevisions.getOrPut(branch) { TreeSet() }.add(revision)
					}
				}
		}

		return RepositoryInformation(latestRevision, branchRevisions, branchCreationPoints)
	}

	private fun findBranchByPathAndRevision(path: String, revision: Long): String? =
		branchDefinitions
			.mapValues { (_, branchDefinition) -> branchDefinition.pathAt(revision) }
			.filter { it.value != null }
			.filterValues { p -> p == path }
			.keys.singleOrNull()
			?: branchDefinitions
				.mapValues { (_, branchDefinition) -> branchDefinition.pathAt(revision) }
				.filter { it.value != null }
				.filterValues { p -> path.startsWith(p!!) }
				.keys.maxByOrNull { it.length }

	private fun findPathForBranchAtRevision(branch: String, revision: Long): String? =
		branchDefinitions[branch]!!.pathAt(revision)

	private val simpleSvn = SimpleSVN(svnRepository)

	private val branchDefinitions = mutableMapOf<String, BranchDefinition>()

}

data class RepositoryInformation(val latestRevision: Long, val brachRevisions: Map<String, SortedSet<Long>>, val branchCreationPoints: Map<String, Pair<String, Long>>)

private operator fun Pair<Long, Long>.contains(value: Long) =
	if (second != -1L) {
		value in first..second
	} else {
		value >= first
	}
