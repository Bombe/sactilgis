package net.pterodactylus.sactilgis

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import net.pterodactylus.utils.svn.BranchDefinition
import net.pterodactylus.utils.svn.RepositoryScanner
import net.pterodactylus.utils.svn.SimpleSVN
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.io.SVNRepositoryFactory
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.core.wc.SVNStatusType.STATUS_NORMAL
import java.io.File

fun main(vararg arguments: String) {

	val configuration = arguments.map(::File).filter(File::exists)
		.map { xmlMapper.readValue(it, Configuration::class.java) }
		.reduceOrNull { mergedConfiguration, configuration -> mergedConfiguration.merge(configuration) }
		?: throw IllegalStateException("No configuration(s) given.")

	val svnUrl = SVNURL.parseURIDecoded(configuration.general.subversionUrl ?: throw IllegalStateException("Subversion URL not set."))
	val svnRepository = SVNRepositoryFactory.create(svnUrl)
	configuration.general.subversionAuth?.let { subversionAuth ->
		subversionAuth.username?.let { username ->
			subversionAuth.password?.let { password ->
				svnRepository.authenticationManager = BasicAuthenticationManager.newInstance(username, password.toCharArray())
			}
		} ?: throw IllegalStateException("Username and Password not given.")
	}
	configuration.branches.takeIf(List<*>::isNotEmpty) ?: throw IllegalStateException("No branches configured.")
	val branchDefinitions = configuration.branches.associate { branch -> branch.name to BranchDefinition(*branch.revisionPaths.map { it.revision to it.path }.toTypedArray()) }
	val repositoryScanner = RepositoryScanner(svnRepository)
	branchDefinitions.forEach(repositoryScanner::addBranch)
	val repositoryInformation = repositoryScanner.identifyBranches()

	val committers = configuration.committers
		.associate { it.subversionId to PersonIdent(it.name, it.email) }
		.withDefault { PersonIdent(it, "$it@svn") }
	val committer = configuration.general.committer?.let { PersonIdent(it.name, it.email) }

	fun findActualRevision(branch: String, revision: Long) =
		repositoryInformation.brachRevisions[branch]!!.headSet(revision + 1).last()

	fun findBranchByPathAndRevision(path: String, revision: Long) =
		branchDefinitions.entries.single { entry ->
			(entry.value.pathAt(revision) == path) || entry.value.pathAt(revision)?.startsWith("$path/") ?: false
		}.key

	val mergeRevisionsByBranch = configuration.branches.associate { it.name to it.merges.associateBy { it.revision } }
	val tagRevisionsByBranch = configuration.branches.associate { branch -> branch.name to branch.tags.associateBy { findActualRevision(branch.name, it.revision) } }
	val fixRevisionsByBranch = configuration.branches.associate { branch -> branch.name to branch.fixes.associateBy { it.revision } }

	val worklist = Worklist(
		repositoryInformation.brachRevisions,
		repositoryInformation.branchCreationPoints.mapValues { entry -> findBranchByPathAndRevision(entry.value.first, entry.value.second) to entry.value.second },
		mergeRevisionsByBranch.mapValues { it.value.mapValues { it.value.branch to it.value.revision } }
	)

	val workDirectory = File(configuration.general.targetDirectory ?: throw IllegalStateException("No target directory given."))
	workDirectory.deleteRecursively()
	workDirectory.mkdirs()
	Git.init().setBare(false).setDirectory(workDirectory).setInitialBranch("main").call().use { gitRepository ->
		if (configuration.general.ignoreGlobalGitIgnoreFile != false) {
			gitRepository.repository.config.setString("core", null, "excludesFile", "<none>")
		}
		val simpleSvn = SimpleSVN(svnRepository)
		val svnClientManager = SVNClientManager.newInstance()
		svnClientManager.setAuthenticationManager(svnRepository.authenticationManager)
		val revisionCommits = mutableMapOf<Long, RevCommit>()
		var currentBranch = "main"
		var currentPath = "/"
		svnClientManager.updateClient.doCheckout(svnUrl, workDirectory, SVNRevision.create(1), SVNRevision.create(1), SVNDepth.EMPTY, false)

		worklist.createPlan().forEach { (branch, revision) ->
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
				printTime("clean") {
					val statusLogs = mutableListOf<SVNStatus>()
					svnClientManager.statusClient.doStatus(workDirectory, svnRevision, SVNDepth.INFINITY, false, true, false, false, statusLogs::add, null)
					statusLogs
						.filterNot { it.file == File(workDirectory, ".git") }
						.filterNot { it.contentsStatus == STATUS_NORMAL }
						.map(SVNStatus::getFile)
						.forEach(File::deleteRecursively)
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
			filePatterns.takeIf { it.isNotEmpty() }?.let {
				printTime("add") {
					gitRepository.add().apply { it.forEach(this::addFilepattern) }.setUpdate(true).call()
					gitRepository.add().apply { it.forEach(this::addFilepattern) }.setUpdate(false).call()
				}
			}
			printTime("commit") {
				val logEntry = simpleSvn.getLogEntry(path, revision)!!
				val commitMessage = (fixRevisionsByBranch[branch]!![revision]?.message?.replaceLineBreaks() ?: logEntry.message) +
						"\n\nSubversion-Original-Commit: $svnUrl$path@$revision\nSubversion-Original-Author: ${logEntry.author}"
				val commitAuthor = committers.getValue(logEntry.author)
				mergeRevisionsByBranch[branch]!![revision]?.let { merge ->
					val commitId = revisionCommits[findActualRevision(merge.branch, revision)]!!
					gitRepository.repository.writeMergeHeads(listOf(commitId))
				}
				val commit = gitRepository.commit()
					.setAllowEmpty(true)
					.setAuthor(PersonIdent(commitAuthor, logEntry.date))
					.setCommitter((committer ?: commitAuthor).let { if (configuration.general.useCommitDateFromEntry != false) PersonIdent(it, logEntry.date) else it })
					.setMessage(commitMessage)
					.setSign(configuration.general.signCommits == true)
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
						.setTagger(PersonIdent(committer ?: committers.getValue(tagLogEntry.author), tagLogEntry.date))
						.setAnnotated(true).setSigned(configuration.general.signCommits == true).call()
				}
			}
			println()
		}
	}
}

private fun String.replaceLineBreaks() = replace(Regex("\\\\n"), "\n")

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
