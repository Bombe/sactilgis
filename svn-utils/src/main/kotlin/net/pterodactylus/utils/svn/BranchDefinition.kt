package net.pterodactylus.utils.svn

import java.util.TreeMap

class BranchDefinition(vararg revisionPaths: Pair<Long, String>) {

	fun pathAt(revision: Long) = revisionPaths.floorEntry(revision)?.value

	private val revisionPaths = TreeMap(mapOf(*revisionPaths))

}
