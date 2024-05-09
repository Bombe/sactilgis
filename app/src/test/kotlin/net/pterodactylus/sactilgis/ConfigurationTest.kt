package net.pterodactylus.sactilgis

import net.pterodactylus.sactilgis.Configuration.Branch
import net.pterodactylus.sactilgis.Configuration.Branch.Fix
import net.pterodactylus.sactilgis.Configuration.Branch.Merge
import net.pterodactylus.sactilgis.Configuration.Branch.Origin
import net.pterodactylus.sactilgis.Configuration.Branch.RevisionPath
import net.pterodactylus.sactilgis.Configuration.Branch.Tag
import net.pterodactylus.sactilgis.Configuration.Committer
import net.pterodactylus.sactilgis.Configuration.General
import net.pterodactylus.sactilgis.Configuration.SubversionAuth
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

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
	fun `merge overwrites subversion auth`() {
		val oldConfiguration = Configuration(general = General(subversionAuth = SubversionAuth("old-user", "old-password")))
		val newConfiguration = Configuration(general = General(subversionAuth = SubversionAuth("new-user", "new-password")))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.subversionAuth, equalTo(SubversionAuth("new-user", "new-password")))
	}

	@Test
	fun `merge does not overwrite subversion auth if new subversion auth is null`() {
		val oldConfiguration = Configuration(general = General(subversionAuth = SubversionAuth("old-user", "old-password")))
		val newConfiguration = Configuration(general = General(subversionAuth = null))
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.general.subversionAuth, equalTo(SubversionAuth("old-user", "old-password")))
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

	@Test
	fun `merge concats the committers`() {
		val oldConfiguration = Configuration().apply {
			committers += listOf(
				Committer("a", "A A", "a@a"),
				Committer("b", "B B", "b@b"),
			)
		}
		val newConfiguration = Configuration().apply {
			committers += listOf(
				Committer("c", "C C", "c@c"),
				Committer("d", "D D", "d@d"),
			)
		}
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(
			mergedConfiguration.committers, containsInAnyOrder(
				Committer("a", "A A", "a@a"),
				Committer("b", "B B", "b@b"),
				Committer("c", "C C", "c@c"),
				Committer("d", "D D", "d@d"),
			)
		)
	}

	@Test
	fun `merge uses new committers when the subversion ID matches`() {
		val oldConfiguration = Configuration().apply {
			committers += listOf(
				Committer("a", "A A", "a@a"),
				Committer("b", "B B", "b@b"),
			)
		}
		val newConfiguration = Configuration().apply {
			committers += listOf(
				Committer("a", "C C", "c@c"),
				Committer("d", "D D", "d@d"),
			)
		}
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(
			mergedConfiguration.committers, containsInAnyOrder(
				Committer("a", "C C", "c@c"),
				Committer("b", "B B", "b@b"),
				Committer("d", "D D", "d@d"),
			)
		)
	}

	@Test
	fun `branches are merged by copying from the new configuration`() {
		val oldConfiguration = Configuration().apply {
			branches += listOf(
				Branch().apply { name = "branch1" },
				Branch().apply { name = "branch2" },
			)
		}
		val newConfiguration = Configuration().apply {
			branches += listOf(
				Branch().apply { name = "branch3" },
				Branch().apply { name = "branch4" },
			)
		}
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.branches.map(Branch::name), contains("branch3", "branch4"))
	}

	@Test
	fun `merge does not overwrite branches with empty branches`() {
		val oldConfiguration = Configuration().apply {
			branches += listOf(
				Branch().apply { name = "branch1" },
				Branch().apply { name = "branch2" },
			)
		}
		val newConfiguration = Configuration()
		val mergedConfiguration = oldConfiguration.merge(newConfiguration)
		assertThat(mergedConfiguration.branches.map(Branch::name), contains("branch1", "branch2"))
	}

	@Test
	fun `(almost) new configuration verifies successfully`() {
		val configuration = Configuration().apply {
			branches.add(Branch().apply {
				name = "init"
				revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
			})
		}
		assertDoesNotThrow(configuration::verify)
	}

	@Test
	fun `configuration with all features used verifies successfully`() {
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "main"
					revisionPaths.add(RevisionPath().apply { revision = 3; path = "/trunk" })
					merges.add(Merge().apply { revision = 8; branch = "feature-1" })
				},
				Branch().apply {
					name = "feature-1"
					origin = Origin(branch = "main", revision = 2)
					revisionPaths.add(RevisionPath().apply { revision = 4; path = "/branches/feature-1" })
					tags.add(Tag().apply { name = "tag-feature"; revision = 5; messageRevision = 6 })
				},
				Branch().apply {
					name = "feature-2"
					origin = Origin(tag = "tag-feature")
					revisionPaths.add(RevisionPath().apply { revision = 8; path = "/branches/feature-2" })
					fixes.add(Fix().apply { revision = 12; message = "New message" })
				}
			)
		}
		assertDoesNotThrow(configuration::verify)
	}

	@Test
	fun `verify throws exception if branch name contains space`() {
		val configuration = Configuration().apply {
			branches += listOf(Branch().apply { name = "other branch" })
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if branch has no revision paths`() {
		val configuration = Configuration().apply {
			branches += listOf(Branch().apply { name = "branch" })
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if tags with spaces are found`() {
		// actually, Git disallows a bunch of things in ref names; check man git-check-ref-format for details.
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "first"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
					tags.add(Tag().apply { name = "tag 1"; revision = 2; messageRevision = 3 })
				}
			)
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if origin tags with spaces are found`() {
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "first"
					origin = Origin(tag = "wrong tag")
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
				}
			)
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if duplicate tags are defined`() {
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "first"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
					tags.add(Tag().apply { name = "tag1"; revision = 2; messageRevision = 3 })
					tags.add(Tag().apply { name = "tag1"; revision = 4; messageRevision = 5 })
				}
			)
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if origin tag does not exist`() {
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "first"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
					tags.add(Tag().apply { name = "tag1"; revision = 2; messageRevision = 3 })
				},
				Branch().apply {
					name = "second"
					origin = Origin(tag = "wrong-tag")
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
				}
			)
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if duplicate branches are defined`() {
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "first"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
				},
				Branch().apply {
					name = "first"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
				}
			)
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if branch to merge does not exist`() {
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "first"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
				},
				Branch().apply {
					name = "second"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
					merges.add(Merge().apply { revision = 3; branch = "wrong" })
				}
			)
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

	@Test
	fun `verify throws exception if origin branch does not exist`() {
		val configuration = Configuration().apply {
			branches += listOf(
				Branch().apply {
					name = "first"
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
				},
				Branch().apply {
					name = "second"
					origin = Origin(branch = "wrong branch", revision = 1)
					revisionPaths.add(RevisionPath().apply { revision = 1; path = "/" })
				}
			)
		}
		assertThrows<IllegalStateException>(configuration::verify)
	}

}
