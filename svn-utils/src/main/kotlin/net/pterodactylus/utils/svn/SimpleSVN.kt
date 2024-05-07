package net.pterodactylus.utils.svn

import org.tmatesoft.svn.core.SVNCommitInfo
import org.tmatesoft.svn.core.SVNErrorCode
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNLogEntry
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.diff.SVNDeltaGenerator
import org.tmatesoft.svn.core.wc.SVNWCUtil
import java.io.InputStream

/**
 * (Hopefully) Simple-to-use wrapper around SVNKit.
 */
class SimpleSVN(private val svnRepository: SVNRepository) {

	fun createCommit(username: String, logMessage: String, commitConsumer: (SimpleCommit) -> Unit) =
		SimpleCommit(username, logMessage).also(commitConsumer).commit()

	fun getLogEntry(path: String, revision: Long): SVNLogEntry? {
		val logEntries = mutableListOf<SVNLogEntry>()
		try {
			svnRepository.log(arrayOf(path), revision, revision, true, false, logEntries::add)
			return logEntries.singleOrNull()
		} catch (svnException: SVNException) {
			if (svnException.errorMessage.errorCode == SVNErrorCode.FS_NOT_FOUND) {
				return null
			}
			throw svnException
		}
	}

	inner class SimpleCommit(private val username: String, private val logMessage: String) {

		fun addDirectory(path: String) {
			directoriesToAdd.add(path)
		}

		fun copyDirectory(path: String, source: String, revision: Long) {
			directoriesToCopy.add(Triple(path, source, revision))
		}

		fun addFile(path: String, content: InputStream) {
			filesToAdd.add(path)
			editFile(path, content)
		}

		fun editFile(path: String, content: InputStream) {
			filesToEdit.add(path to content)
		}

		fun copyFile(path: String, source: String, revision: Long) {
			filesToCopy.add(Triple(path, source, revision))
		}

		fun deletePath(path: String) {
			pathsToDelete.add(path)
		}

		internal fun commit(): SVNCommitInfo {
			svnRepository.authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(username, charArrayOf())
			val latestRevision: Long = svnRepository.latestRevision
			val commitEditor = svnRepository.getCommitEditor(logMessage, null, false, null)
			commitEditor.openRoot(latestRevision)
			directoriesToAdd.forEach { path -> commitEditor.addDir(path, null, -1) }
			directoriesToCopy.forEach { (path, source, revision) -> commitEditor.addDir(path, source, revision) }
			filesToCopy.forEach { (path, source, revision) -> commitEditor.addFile(path, source, revision) }
			filesToAdd.forEach { path ->
				commitEditor.addFile(path, null, -1)
			}
			filesToEdit.forEach { (path, content) ->
				commitEditor.applyTextDelta(path, null)
				SVNDeltaGenerator().sendDelta(path, content, commitEditor, true)
			}
			pathsToDelete.forEach { path -> commitEditor.deleteEntry(path, -1) }
			return commitEditor.closeEdit()
		}

		private val directoriesToAdd = mutableListOf<String>()
		private val directoriesToCopy = mutableListOf<Triple<String, String, Long>>()
		private val filesToAdd = mutableListOf<String>()
		private val filesToEdit = mutableListOf<Pair<String, InputStream>>()
		private val filesToCopy = mutableListOf<Triple<String, String, Long>>()
		private val pathsToDelete = mutableListOf<String>()

	}

}
