package net.pterodactylus.sactilgis

import java.util.SortedSet

class Worklist(private val branchRevisions: Map<String, SortedSet<Long>> = emptyMap(), private val branchCreationPoints: Map<String, Pair<String, Long>> = emptyMap(), private val branchMergePoints: Map<String, Map<Long, Pair<String, Long>>> = emptyMap()) {

	fun createPlan(): List<Pair<String, Long>> {
		val branchRevisionPool = branchRevisions.map { (key, value) -> key to value.toSortedSet() }.toMap().toMutableMap()
		var lastBranch = ""
		return branchMergePoints
			.flatMap { branchMergePoint ->
				branchMergePoint.value.map { it.key to MergeInformation(branchMergePoint.key, it.value.first, it.value.second) }
			}
			.sortedBy { it.first }
			.flatMap { (revision, mergeInformation) ->
				val revisionsToProcess = mutableListOf<Pair<String, Long>>()
				branchCreationPoints[mergeInformation.targetBranch]?.let { targetBranchCreationPoint ->
					if (targetBranchCreationPoint.second in branchRevisionPool[targetBranchCreationPoint.first]!!) {
						revisionsToProcess += branchRevisionPool[targetBranchCreationPoint.first]!!.headSet(targetBranchCreationPoint.second + 1).map { targetBranchCreationPoint.first to it }
						branchRevisionPool[targetBranchCreationPoint.first]!! -= revisionsToProcess.map(Pair<*, Long>::second)
						lastBranch = targetBranchCreationPoint.first
					}
				}
				val revisionsFromTargetBranch = branchRevisionPool[mergeInformation.targetBranch]!!.headSet(revision).toSortedSet()
				val revisionsFromSourceBranch = branchRevisionPool[mergeInformation.sourceBranch]!!.headSet(revision + 1).toSortedSet()
				branchRevisionPool[mergeInformation.targetBranch]!! -= revisionsFromTargetBranch
				branchRevisionPool[mergeInformation.sourceBranch]!! -= revisionsFromSourceBranch
				if (lastBranch == mergeInformation.sourceBranch) {
					revisionsToProcess += revisionsFromSourceBranch.map { mergeInformation.sourceBranch to it }
					revisionsToProcess += revisionsFromTargetBranch.map { mergeInformation.targetBranch to it }
					lastBranch = mergeInformation.targetBranch
				} else {
					revisionsToProcess += revisionsFromTargetBranch.map { mergeInformation.targetBranch to it }
					revisionsToProcess += revisionsFromSourceBranch.map { mergeInformation.sourceBranch to it }
					lastBranch = mergeInformation.sourceBranch
				}
				revisionsToProcess
			}
			.plus(branchRevisionPool.flatMap { entry -> entry.value.map { entry.key to it } }.sortedWith { (leftBranch, leftRevision), (rightBranch, rightRevision) ->
				if (branchCreationPoints[leftBranch]?.first == rightBranch) {
					1
				} else if (branchCreationPoints[rightBranch]?.first == leftBranch) {
					-1
				} else {
					leftBranch.compareTo(rightBranch).takeIf { it != 0 } ?: leftRevision.compareTo(rightRevision)
				}
			})
	}

}

data class MergeInformation(val targetBranch: String, val sourceBranch: String, val sourceRevision: Long)
