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

	fun createCommit(username: String, commitConsumer: (SimpleCommit) -> Unit): SVNCommitInfo {
		val simpleCommit = SimpleCommit(username)
		commitConsumer(simpleCommit)
		return simpleCommit.commit()
	}

	private val svnRepository: SVNRepository = SVNRepositoryFactory.create(svnUrl)

	inner class SimpleCommit(private val username: String) {

		internal fun commit(): SVNCommitInfo {
			svnRepository.authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(username, charArrayOf())
			val latestRevision: Long = svnRepository.latestRevision
			val commitEditor = svnRepository.getCommitEditor("", null, false, null)
			commitEditor.openRoot(latestRevision)
			return commitEditor.closeEdit()
		}

	}

}
