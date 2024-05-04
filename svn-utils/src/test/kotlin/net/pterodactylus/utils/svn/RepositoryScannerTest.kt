package net.pterodactylus.utils.svn

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasEntry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class RepositoryScannerTest {

	@Test
	fun `repository scanner returns latest revision of repository`() {
		createTwoSimpleRepositories()
		val repositoryInformation = repositoryScanner.identifyBranches()
		assertThat(repositoryInformation.latestRevision, equalTo(4))
	}

	@Test
	fun `repository scanner identifies branch correctly`() {
		createTwoSimpleRepositories()
		repositoryScanner.addBranch("p1", 0L to "/project1")
		repositoryScanner.addBranch("p2", 0L to "/project2")
		val repositoryInformation = repositoryScanner.identifyBranches()
		assertThat(
			repositoryInformation.brachRevisions, allOf(
				hasEntry(equalTo("p1"), contains(1, 3)),
				hasEntry(equalTo("p2"), contains(2, 4))
			)
		)
	}

	@Test
	fun `repository scanner can handle outside-of-any-project revisions`() {
		createTwoSimpleRepositories()
		repositoryScanner.addBranch("p1", 0L to "/project1")
		val repositoryInformation = repositoryScanner.identifyBranches()
		assertThat(
			repositoryInformation.brachRevisions, allOf(
				hasEntry(equalTo("p1"), contains(1, 3))
			)
		)
	}

	@Test
	fun `repository scanner can handle project that is moving around in repository`() {
		createTwoSimpleRepositories()
		simpleSvn.createCommit("testuser", "adding standard layout") { commit ->
			commit.addDirectory("/branches")
			commit.addDirectory("/tags")
			commit.addDirectory("/trunk")
		}
		simpleSvn.createCommit("testuser", "move project1") { commit ->
			commit.copyDirectory("/trunk/project1", "/project1", 5)
			commit.deletePath("/project1")
		}
		repositoryScanner.addBranch("p1", 0L to "/project1", 6L to "/trunk/project1")
		val repositoryInformation = repositoryScanner.identifyBranches()
		assertThat(
			repositoryInformation.brachRevisions, allOf(
				hasEntry(equalTo("p1"), contains(1, 3, 6))
			)
		)
	}

	@Test
	fun `repository scanner can handle branch that does not exist at start of repository`() {
		createTwoSimpleRepositories()
		repositoryScanner.addBranch("p2", 2L to "/project2")
		val repositoryInformation = repositoryScanner.identifyBranches()
		assertThat(
			repositoryInformation.brachRevisions, allOf(
				hasEntry(equalTo("p2"), contains(2, 4))
			)
		)
	}

	@Test
	fun `repository scanner identifies branch creation points correctly`() {
		simpleSvn.createCommit("testuser", "create directory") { it.addDirectory("/main") }
		simpleSvn.createCommit("testuser", "create directory") { it.addDirectory("/main/foo") }
		simpleSvn.createCommit("testuser", "create directory") { it.addDirectory("/main/bar") }
		simpleSvn.createCommit("testuser", "create directory") { it.copyDirectory("/foo", "/main/foo", 2) }
		simpleSvn.createCommit("testuser", "create directory") { it.addDirectory("/main/baz") }
		simpleSvn.createCommit("testuser", "create directory") { it.addDirectory("/foo/foo2") }
		simpleSvn.createCommit("testuser", "create directory") { it.copyDirectory("/bar", "/main/bar", 6) }
		repositoryScanner.addBranch("main", 0L to "/main")
		repositoryScanner.addBranch("foo", 4L to "/foo")
		repositoryScanner.addBranch("bar", 7L to "/bar")
		val repositoryInformation = repositoryScanner.identifyBranches()
		assertThat(repositoryInformation.branchCreationPoints, allOf(
			hasEntry(equalTo("foo"), equalTo("/main/foo" to 2L)),
			hasEntry(equalTo("bar"), equalTo("/main/bar" to 5L))
		))
	}

	private fun createTwoSimpleRepositories() {
		simpleSvn.createCommit("testuser", "create directory") { commit ->
			commit.addDirectory("/project1")
			commit.addFile("/project1/README", "project 1\n".byteInputStream())
		}
		simpleSvn.createCommit("testuser", "add second project") { commit ->
			commit.addDirectory("/project2")
			commit.addFile("/project2/README", "project 2\n".byteInputStream())
		}
		simpleSvn.createCommit("testuser", "add file to first project") { it.addFile("/project1/file1", "file 1\n".byteInputStream()) }
		simpleSvn.createCommit("testuser", "add file to second project") { it.addFile("/project2/file2", "file 2\n".byteInputStream()) }
	}

	private fun createSvnRepository(directory: File): SVNURL =
		SVNRepositoryFactory.createLocalRepository(Files.createTempDirectory(directory.toPath(), "svn-repo").toFile(), false, false)

	@TempDir
	private lateinit var temporaryDirectory: Path
	private val svnUrl by lazy { createSvnRepository(Files.createTempDirectory(temporaryDirectory, "svn-repo").toFile()) }
	private val simpleSvn by lazy { SimpleSVN(svnUrl) }
	private val repositoryScanner by lazy { RepositoryScanner(svnUrl) }

}
