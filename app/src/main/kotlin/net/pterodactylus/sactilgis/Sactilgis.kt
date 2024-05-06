package net.pterodactylus.sactilgis

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import net.pterodactylus.utils.svn.BranchDefinition
import net.pterodactylus.utils.svn.RepositoryScanner
import net.pterodactylus.utils.svn.SimpleSVN
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeCommand.FastForwardMode
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision
import java.io.File

fun main(vararg arguments: String) {

	val configuration = xmlMapper.readValue(File(arguments.first()), Configuration::class.java)

	val svnUrl = SVNURL.parseURIDecoded(configuration.general.subversionUrl)
	val branchDefinitions = configuration.branches.associate { branch -> branch.name to BranchDefinition(*branch.revisionPaths.map { it.revision to it.path }.toTypedArray()) }
	val repositoryScanner = RepositoryScanner(svnUrl)
	branchDefinitions.forEach(repositoryScanner::addBranch)
	val repositoryInformation = repositoryScanner.identifyBranches()

	val committers = configuration.committers
		.associate { it.subversionId to PersonIdent(it.name, it.email) }
		.withDefault { PersonIdent("Unknown", "unknown@svn") }
	val committer = configuration.general.committer.let { PersonIdent(it.name, it.email) }

	fun findActualRevision(branch: String, revision: Long) =
		repositoryInformation.brachRevisions[branch]!!.headSet(revision + 1).last()

	val mergeRevisionsByBranch = configuration.branches.associate { it.name to it.merges.associateBy { it.revision } }
	val tagRevisionsByBranch = configuration.branches.associate { branch -> branch.name to branch.tags.associateBy { findActualRevision(branch.name, it.revision) } }
	val fixRevisionsByBranch = configuration.branches.associate { branch -> branch.name to branch.fixes.associateBy { it.revision } }

	val workDirectory = File(configuration.general.targetDirectory)
	workDirectory.deleteRecursively()
	workDirectory.mkdirs()
	Git.init().setBare(false).setDirectory(workDirectory).setInitialBranch("main").call().use { gitRepository ->
		if (configuration.general.ignoreGlobalGitIgnoreFile) {
			gitRepository.repository.config.setString("core", null, "excludesFile", "<none>")
		}
		val simpleSvn = SimpleSVN(svnUrl)
		val svnClientManager = SVNClientManager.newInstance()
		val revisionCommits = mutableMapOf<Long, RevCommit>()
		var currentBranch = "main"
		var currentPath = "/"
		svnClientManager.updateClient.doCheckout(svnUrl, workDirectory, SVNRevision.create(1), SVNRevision.create(1), SVNDepth.EMPTY, false)

		repositoryInformation.brachRevisions.flatMap { (key, value) -> value.map { it to key } }.sortedBy { it.first }.forEach { (revision, branch) ->
			print("${"%tT.%<tL".format(System.currentTimeMillis())} (@$revision)($branch)")
			val svnRevision = SVNRevision.create(revision)
			if (currentBranch != branch) {
				if (gitRepository.branchDoesNotExist(branch)) {
					print("(creating $branch)")
					val originalRevision = repositoryInformation.branchCreationPoints[branch]?.second
					if (originalRevision != null) {
						printTime("create") {
							gitRepository.checkout().setCreateBranch(true).setName(branch).setStartPoint(revisionCommits[originalRevision]!!.name).call()
						}
					} else {
						printTime("orphan") {
							gitRepository.checkout().setName(branch).setOrphan(true).call()
						}
					}
				} else {
					print("(switching to $branch)")
					printTime("checkout") {
						gitRepository.checkout().setName(branch).call()
					}
				}
				currentBranch = branch
			}
			mergeRevisionsByBranch[branch]!![revision]?.let { merge ->
				print("(merging ${merge.branch})")
				printTime("merge") {
					gitRepository.merge().setFastForward(FastForwardMode.NO_FF).include(gitRepository.repository.findRef(merge.branch)).setCommit(false).call()
				}
			}
			val path = branchDefinitions[branch]!!.pathAt(revision)!!
			print("($path)")
			if (path != currentPath) {
				currentPath = path
				printTime("switch") {
					svnClientManager.updateClient.doSwitch(workDirectory, svnUrl.appendPath(path, false), svnRevision, svnRevision, SVNDepth.INFINITY, false, true)
				}
			} else {
				printTime("update") {
					svnClientManager.updateClient.doUpdate(workDirectory, svnRevision, SVNDepth.INFINITY, false, true)
				}
			}
			val filePatterns = printTime("status") {
				gitRepository.status().call().let { status ->
					status.added + status.changed + status.modified + status.missing + status.removed + status.untracked + status.untrackedFolders + status.ignoredNotInIndex
				}.filterNot { it.startsWith(".svn") }
			}
			print("(${filePatterns.size} files)")
			filePatterns.takeIf { it.isNotEmpty() }?.let {
				printTime("add") {
					gitRepository.add().apply { it.forEach(this::addFilepattern) }.setUpdate(true).call()
					gitRepository.add().apply { it.forEach(this::addFilepattern) }.setUpdate(false).call()
				}
			}
			printTime("commit") {
				val logEntry = simpleSvn.getLogEntry(path, revision)!!
				val commitMessage = (fixRevisionsByBranch[branch]!![revision]?.message ?: logEntry.message) +
						"\n\nSubversion-Original-Commit: $svnUrl$path@$revision\nSubversion-Original-Author: ${logEntry.author}"
				val commit = gitRepository.commit()
					.setAllowEmpty(true)
					.setAuthor(PersonIdent(committers.getValue(logEntry.author), logEntry.date))
					.setCommitter(committer.let { if (configuration.general.useCommitDateFromEntry) PersonIdent(it, logEntry.date) else it })
					.setMessage(commitMessage)
					.setSign(false)
					.call()
				revisionCommits[revision] = commit
				print("(${commit.id.name})")
			}
			tagRevisionsByBranch[branch]!![revision]?.let { tag ->
				print("(tagging ${tag.name})")
				val tagLogEntry = simpleSvn.getLogEntry("/", tag.messageRevision)!!
				printTime("tag") {
					gitRepository.tag()
						.setObjectId(revisionCommits[revision])
						.setName(tag.name)
						.setMessage(tagLogEntry.message)
						.setTagger(PersonIdent(committer, tagLogEntry.date))
						.setAnnotated(true).setSigned(false).call()
				}
			}
			println()
		}
	}
}

private fun <T : Any> printTime(text: String, action: () -> T): T {
	val timeBefore = System.currentTimeMillis()
	try {
		return action()
	} finally {
		val timeAfter = System.currentTimeMillis()
		print("($text: ${(timeAfter - timeBefore) / 1000.0}s)")
	}
}

private fun Git.branchDoesNotExist(branch: String) =
	"refs/heads/$branch" !in branchList().call().map(Ref::getName)

private val xmlMapper = XmlMapper()
