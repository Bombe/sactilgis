package net.pterodactylus.utils.svn

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tmatesoft.svn.core.SVNLogEntry
import org.tmatesoft.svn.core.SVNNodeKind
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNLocationEntry
import org.tmatesoft.svn.core.io.SVNRepository
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class SimpleSVNTest {

	@Test
	fun `SimpleSVN can be created`() {
		assertThat(simpleSvn, notNullValue())
	}

	@Test
	fun `SimpleSVN can create commit with username but without content`() {
		val commitInfo = simpleSvn.createCommit("testuser", "") { _ ->
		}
		assertThat(commitInfo.newRevision, equalTo(1))
		assertThat(commitInfo.author, equalTo("testuser"))
	}

	@Test
	fun `SimpleSVN can create commit with username and log message but without content`() {
		simpleSvn.createCommit("testuser", "first commit") { _ ->
		}
		val logEntries = mutableListOf<SVNLogEntry>()
		svnRepository.log(arrayOf("/"), -1, -1, false, false, logEntries::add)
		assertThat(logEntries.single().message, equalTo("first commit"))
	}

	@Test
	fun `SimpleSVN can create directory in commit`() {
		addTestDirectory()
		val nodeKind = svnRepository.checkPath("/test", 1)
		assertThat(nodeKind, equalTo(SVNNodeKind.DIR))
	}

	@Test
	fun `SimpleSVN can copy directory in commit`() {
		addTestDirectory()
		copyTestDirectoryToTest2()
		val nodeKind = svnRepository.checkPath("/test2", 2)
		assertThat(nodeKind, equalTo(SVNNodeKind.DIR))
	}

	@Test
	fun `SimpleSVN can copy directory in commit while keeping history`() {
		addTestDirectory()
		copyTestDirectoryToTest2()
		val locationEntries = mutableListOf<SVNLocationEntry>()
		svnRepository.getLocations("/test2", 2, longArrayOf(1), locationEntries::add)
		assertThat(locationEntries.single().path, equalTo("/test"))
	}

	@Test
	fun `SimpleSVN can create file in repository`() {
		addREADME()
		val nodeKind = svnRepository.checkPath("/README", 1)
		assertThat(nodeKind, equalTo(SVNNodeKind.FILE))
	}

	@Test
	fun `SimpleSVN can create file with content in repository`() {
		addREADME()
		assertThat(getFileContent("/README", 1), equalTo("the best project ever!\n"))
	}

	@Test
	fun `SimpleSVN can copy file in repository`() {
		addREADME()
		copyREADMEtoREADMEmd()
		val nodeKind = svnRepository.checkPath("/README.md", 2)
		assertThat(nodeKind, equalTo(SVNNodeKind.FILE))
	}

	@Test
	fun `SimpleSVN can copy file with content in repository`() {
		addREADME()
		copyREADMEtoREADMEmd()
		assertThat(getFileContent("/README.md", 2), equalTo("the best project ever!\n"))
	}

	@Test
	fun `SimpleSVN can edit file in repository`() {
		addREADME()
		simpleSvn.createCommit("testuser", "edit README") { commit ->
			commit.editFile("/README", "really, best project.\n".byteInputStream())
		}
		assertThat(getFileContent("/README", 2), equalTo("really, best project.\n"))
	}

	@Test
	fun `SimpleSVN can delete file in repository`() {
		addREADME()
		simpleSvn.createCommit("testuser", "delete README") { commit ->
			commit.deletePath("/README")
		}
		val nodeKind = svnRepository.checkPath("/README", 2)
		assertThat(nodeKind, equalTo(SVNNodeKind.NONE))
	}

	@Test
	fun `SimpleSVN can delete directory in repository`() {
		addTestDirectory()
		simpleSvn.createCommit("testuser", "delete README") { commit ->
			commit.deletePath("/test")
		}
		val nodeKind = svnRepository.checkPath("/test", 2)
		assertThat(nodeKind, equalTo(SVNNodeKind.NONE))
	}

	@Test
	fun `SimpleSVN can provide log entry for an existing path in a revision`() {
		addTestDirectory()
		val logEntry = simpleSvn.getLogEntry("/test", 1)
		assertThat(logEntry!!.message, equalTo("add directory"))
	}

	@Test
	fun `SimpleSVN returns null entry if path does not exist in revision`() {
		addTestDirectory()
		val logEntry = simpleSvn.getLogEntry("/test2", 1)
		assertThat(logEntry, nullValue())
	}

	@Test
	fun `SimpleSVN provides changed paths in log entry`() {
		addTestDirectory()
		val logEntry = simpleSvn.getLogEntry("/test", 1)
		assertThat(logEntry!!.changedPaths, not(emptyMap()))
	}

	private fun addTestDirectory() {
		simpleSvn.createCommit("testuser", "add directory") { commit ->
			commit.addDirectory("/test")
		}
	}

	private fun copyTestDirectoryToTest2() {
		simpleSvn.createCommit("testuser", "copy directory") { commit ->
			commit.copyDirectory("/test2", "test", 1);
		}
	}

	private fun addREADME() {
		simpleSvn.createCommit("testuser", "add a file") { commit ->
			commit.addFile("/README", "the best project ever!\n".byteInputStream())
		}
	}

	private fun copyREADMEtoREADMEmd() {
		simpleSvn.createCommit("testuser", "copy README") { commit ->
			commit.copyFile("/README.md", "/README", 1)
		}
	}

	private fun getFileContent(path: String, revision: Long): String = ByteArrayOutputStream().use { outputStream ->
		svnRepository.getFile(path, revision, null, outputStream)
		return outputStream.toByteArray().decodeToString()
	}

	private fun createSvnRepository(directory: File): SVNURL =
		SVNRepositoryFactory.createLocalRepository(Files.createTempDirectory(directory.toPath(), "svn-repo").toFile(), false, false)

	@TempDir
	private lateinit var temporaryDirectory: Path
	private val svnUrl by lazy { createSvnRepository(Files.createTempDirectory(temporaryDirectory, "svn-repo").toFile()) }
	private val svnRepository: SVNRepository by lazy { SVNRepositoryFactory.create(svnUrl) }
	private val simpleSvn by lazy { SimpleSVN(svnRepository) }

}
