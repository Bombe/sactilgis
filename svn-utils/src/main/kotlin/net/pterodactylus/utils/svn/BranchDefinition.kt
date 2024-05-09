package net.pterodactylus.utils.svn

import java.util.TreeMap

class BranchDefinition(vararg revisionPaths: Pair<Long, String>) {

	private val revisionPaths = TreeMap(mapOf(*revisionPaths))

	fun pathAt(revision: Long) = revisionPaths.floorEntry(revision)?.value
	val earliestRevision: Long = this.revisionPaths.firstEntry().key

}
