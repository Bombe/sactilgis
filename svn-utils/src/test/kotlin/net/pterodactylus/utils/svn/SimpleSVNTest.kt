package net.pterodactylus.utils.svn

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import java.io.File
import java.nio.file.Files

class SimpleSVNTest {

	@Test
	fun `SimpleSVN can be created`(@TempDir tempDir: File) {
		SimpleSVN(createSvnRepository(tempDir))
	}

	@Test
	fun `SimpleSVN can create commit with username but without content`(@TempDir tempDir: File) {
		val simpleSvn = SimpleSVN(createSvnRepository(tempDir))
		val commitInfo = simpleSvn.createCommit("testuser") { _ ->
		}
		assertThat(commitInfo.newRevision, equalTo(1))
		assertThat(commitInfo.author, equalTo("testuser"))
	}

	private fun createSvnRepository(directory: File): SVNURL =
		SVNRepositoryFactory.createLocalRepository(Files.createTempDirectory(directory.toPath(), "svn-repo").toFile(), false, false)

}
