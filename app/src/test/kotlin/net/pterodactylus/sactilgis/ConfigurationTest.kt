package net.pterodactylus.sactilgis

import net.pterodactylus.sactilgis.Configuration.Committer
import net.pterodactylus.sactilgis.Configuration.General
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test

class ConfigurationTest {

	@Test
	fun `merge overwrites subversion url`() {
		val oldConfiguration = Configuration(general = General(subversionUrl = "old-svn-url"))
		val newConfiguration = Configuration(general = General(subversionUrl = "new-svn-url"))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.subversionUrl, equalTo("new-svn-url"))
	}

	@Test
	fun `merge does not overwrites subversion url if new url is null`() {
		val oldConfiguration = Configuration(general = General(subversionUrl = "old-svn-url"))
		val newConfiguration = Configuration(general = General(subversionUrl = null))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.subversionUrl, equalTo("old-svn-url"))
	}

	@Test
	fun `merge overwrites committer`() {
		val oldConfiguration = Configuration(general = General(committer = Committer("old-svn-id", "old-name", "old-email")))
		val newConfiguration = Configuration(general = General(committer = Committer("new-svn-id", "new-name", "new-email")))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.committer, equalTo(Committer("new-svn-id", "new-name", "new-email")))
	}

	@Test
	fun `merge does not overwrite committer if new committer is null`() {
		val oldConfiguration = Configuration(general = General(committer = Committer("old-svn-id", "old-name", "old-email")))
		val newConfiguration = Configuration(general = General(committer = null))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.committer, equalTo(Committer("old-svn-id", "old-name", "old-email")))
	}

	@Test
	fun `merge overwrites target directory`() {
		val oldConfiguration = Configuration(general = General(targetDirectory = "old-target-directory"))
		val newConfiguration = Configuration(general = General(targetDirectory = "new-target-directory"))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.targetDirectory, equalTo("new-target-directory"))
	}

	@Test
	fun `merge does not overwrites target directory if new target directory is null`() {
		val oldConfiguration = Configuration(general = General(targetDirectory = "old-target-directory"))
		val newConfiguration = Configuration(general = General(targetDirectory = null))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.targetDirectory, equalTo("old-target-directory"))
	}

	@Test
	fun `merge overwrites use-commit-date flag`() {
		val oldConfiguration = Configuration(general = General(useCommitDateFromEntry = true))
		val newConfiguration = Configuration(general = General(useCommitDateFromEntry = false))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.useCommitDateFromEntry, equalTo(false))
	}

	@Test
	fun `merge does not overwrite use-commit-date flag when new flag is null`() {
		val oldConfiguration = Configuration(general = General(useCommitDateFromEntry = true))
		val newConfiguration = Configuration(general = General(useCommitDateFromEntry = null))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.useCommitDateFromEntry, equalTo(true))
	}

	@Test
	fun `merge overwrites ignore-global-gitignore flag`() {
		val oldConfiguration = Configuration(general = General(ignoreGlobalGitIgnoreFile = true))
		val newConfiguration = Configuration(general = General(ignoreGlobalGitIgnoreFile = false))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.ignoreGlobalGitIgnoreFile, equalTo(false))
	}

	@Test
	fun `merge does not overwrite ignore-global-gitignore flag when new flag is null`() {
		val oldConfiguration = Configuration(general = General(ignoreGlobalGitIgnoreFile = true))
		val newConfiguration = Configuration(general = General(ignoreGlobalGitIgnoreFile = null))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.ignoreGlobalGitIgnoreFile, equalTo(true))
	}

	@Test
	fun `merge overwrites sign-commits flag`() {
		val oldConfiguration = Configuration(general = General(signCommits = true))
		val newConfiguration = Configuration(general = General(signCommits = false))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.signCommits, equalTo(false))
	}

	@Test
	fun `merge does not overwrite sign-commits flag when new flag is null`() {
		val oldConfiguration = Configuration(general = General(signCommits = true))
		val newConfiguration = Configuration(general = General(signCommits = null))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.signCommits, equalTo(true))
	}

}