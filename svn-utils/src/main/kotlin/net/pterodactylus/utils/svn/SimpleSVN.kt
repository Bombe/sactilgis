package net.pterodactylus.utils.svn

import org.tmatesoft.svn.core.SVNCommitInfo
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
import org.tmatesoft.svn.core.wc.SVNWCUtil
import java.io.InputStream

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

		fun copyDirectory(path: String, source: String, revision: Long) {
			directoriesToCopy.add(Triple(path, source, revision))
		}

		fun addFile(path: String, content: InputStream) {
			filesToAdd.add(path to content)
		}

		fun copyFile(path: String, source: String, revision: Long) {
			filesToCopy.add(Triple(path, source, revision))
		}

		internal fun commit(): SVNCommitInfo {
			svnRepository.authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(username, charArrayOf())
			val latestRevision: Long = svnRepository.latestRevision
			val commitEditor = svnRepository.getCommitEditor(logMessage, null, false, null)
			commitEditor.openRoot(latestRevision)
			directoriesToAdd.forEach { path -> commitEditor.addDir(path, null, -1) }
			directoriesToCopy.forEach { (path, source, revision) -> commitEditor.addDir(path, source, revision) }
			filesToCopy.forEach { (path, source, revision) -> commitEditor.addFile(path, source, revision) }
			filesToAdd.forEach { (path, content) ->
				commitEditor.addFile(path, null, -1)
				commitEditor.applyTextDelta(path, null)
				SVNDeltaGenerator().sendDelta(path, content, commitEditor, true)
			}
			return commitEditor.closeEdit()
		}

		private val directoriesToAdd = mutableListOf<String>()
		private val directoriesToCopy = mutableListOf<Triple<String, String, Long>>()
		private val filesToAdd = mutableListOf<Pair<String, InputStream>>()
		private val filesToCopy = mutableListOf<Triple<String, String, Long>>()

	}

}
