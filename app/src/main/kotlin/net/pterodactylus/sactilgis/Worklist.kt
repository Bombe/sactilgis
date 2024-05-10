package net.pterodactylus.sactilgis

import java.util.SortedSet

class Worklist(private val branchRevisions: Map<String, SortedSet<Long>> = emptyMap(), private val branchCreationPoints: Map<String, Pair<String, Long>> = emptyMap(), private val branchMergePoints: Map<String, Map<Long, Pair<String, Long>>> = emptyMap()) {

	fun createPlan(): List<Pair<String, Long>> {
		val nodes = mutableMapOf<Long, MutableList<Long>>()
		var lastBranch = ""
		var lastRevision = -1L
		branchRevisions.flatMap { branchRevision -> branchRevision.value.map { branchRevision.key to it } }.forEach { (branch, revision) ->
			if (lastBranch != branch) {
				nodes[revision] = mutableListOf()
				lastBranch = branch
			} else {
				nodes.getOrPut(revision) { mutableListOf() } += lastRevision
			}
			lastRevision = revision
		}
		branchCreationPoints.forEach { newBranch, (oldBranch, revision) ->
			val firstRevisionOfNewBranch = branchRevisions[newBranch]!!.first()
			val actualRevisionInOldBranch = branchRevisions[oldBranch]!!.headSet(revision + 1).last()
			nodes.getOrPut(firstRevisionOfNewBranch) { mutableListOf() } += actualRevisionInOldBranch
		}
		branchMergePoints.forEach { (targetBranch, merges) ->
			merges.forEach { mergeRevision, (branchToMerge, revisionToMerge) ->
				val actualRevisionInBranchToMerge = branchRevisions[branchToMerge]!!.headSet(revisionToMerge + 1).last()
				nodes.getOrPut(mergeRevision) { mutableListOf() } += actualRevisionInBranchToMerge
			}
		}
		return findDisconnectedGraphs(nodes)
			.map(depthFirstSort(nodes))
			.flatten()
			.map { revision -> findBranchForRevision(revision) to revision }
	}

	// shamelessly taken from https://en.wikipedia.org/wiki/Topological_sorting#Depth-first_search
	private fun depthFirstSort(nodes: MutableMap<Long, MutableList<Long>>) = { graph: Set<Long> ->
		val permanentMarks = mutableSetOf<Long>()
		val temporaryMarks = mutableSetOf<Long>()
		val sortedNodes = mutableListOf<Long>()

		fun visit(node: Long) {
			if (node in permanentMarks) {
				return
			}
			if (node in temporaryMarks) {
				throw IllegalStateException("Cycle detected for $node")
			}

			temporaryMarks += node
			nodes[node]!!.forEach(::visit)
			temporaryMarks -= node

			permanentMarks += node
			sortedNodes.add(node)
		}

		while ((graph - permanentMarks).isNotEmpty()) {
			(graph - permanentMarks).first().let(::visit)
		}

		sortedNodes
	}

	private fun findBranchForRevision(revision: Long) = branchRevisions.entries.first { revision in it.value }.key

	private fun findDisconnectedGraphs(nodes: Map<Long, List<Long>>): Collection<Set<Long>> {
		val graphs = mutableMapOf<Long, MutableSet<Long>>()
		nodes.forEach { (parent, children) ->
			var graph = mutableSetOf<Long>()
			children.forEach { child ->
				if (child in graphs) {
					graph = graphs[child]!!
				}
				graph += child
				graphs[child] = graph
			}
			graph += parent
			graphs[parent] = graph
		}
		return graphs.values.distinct()
	}

}
