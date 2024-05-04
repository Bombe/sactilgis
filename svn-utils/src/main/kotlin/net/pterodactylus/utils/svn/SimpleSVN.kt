package net.pterodactylus.utils.svn

import org.tmatesoft.svn.core.SVNCommitInfo
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.SVNWCUtil

/**
 * (Hopefully) Simple-to-use wrapper around SVNKit.
 */
class SimpleSVN(svnUrl: SVNURL) {

	fun createCommit(username: String, logMessage: String, commitConsumer: (SimpleCommit) -> Unit): SVNCommitInfo {
		val simpleCommit = SimpleCommit(username, logMessage)
		commitConsumer(simpleCommit)
		return simpleCommit.commit()
	}

	private val svnRepository: SVNRepository = SVNRepositoryFactory.create(svnUrl)

	inner class SimpleCommit(private val username: String, private val logMessage: String) {

		fun addDirectory(path: String) {
			directoriesToAdd.add(path)
		}

		internal fun commit(): SVNCommitInfo {
			svnRepository.authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(username, charArrayOf())
			val latestRevision: Long = svnRepository.latestRevision
			val commitEditor = svnRepository.getCommitEditor(logMessage, null, false, null)
			commitEditor.openRoot(latestRevision)
			directoriesToAdd.forEach { path -> commitEditor.addDir(path, null, -1) }
			return commitEditor.closeEdit()
		}

		private val directoriesToAdd = mutableListOf<String>()

	}

}
