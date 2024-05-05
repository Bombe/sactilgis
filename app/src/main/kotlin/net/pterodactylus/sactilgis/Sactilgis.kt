package net.pterodactylus.sactilgis

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import net.pterodactylus.utils.svn.BranchDefinition
import net.pterodactylus.utils.svn.RepositoryScanner
import net.pterodactylus.utils.svn.SimpleSVN
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes

fun main(vararg arguments: String) {

	val configuration = xmlMapper.readValue(File(arguments.first()), Configuration::class.java)

	val svnUrl = SVNURL.parseURIDecoded(configuration.subversion.url)
	val branchDefinitions = configuration.branches.associate { branch -> branch.name to BranchDefinition(*branch.revisionPaths.map { it.revision to it.path }.toTypedArray()) }
	val repositoryScanner = RepositoryScanner(svnUrl)
	branchDefinitions.forEach(repositoryScanner::addBranch)
	val repositoryInformation = repositoryScanner.identifyBranches()

	val committers = configuration.committers
		.associate { it.subversionId to PersonIdent(it.name, it.email) }
		.withDefault { PersonIdent("Unknown", "unknown@svn") }

	val workDirectory = File("git-repo")
	workDirectory.mkdirs()
	Git.init().setBare(false).setDirectory(workDirectory).setInitialBranch("main").call().use { gitRepository ->
		val simpleSvn = SimpleSVN(svnUrl)
		val svnClientManager = SVNClientManager.newInstance()
		val revisionCommits = mutableMapOf<Long, AnyObjectId>()
		var currentBranch = "main"

		repositoryInformation.brachRevisions.flatMap { (key, value) -> value.map { it to key } }.sortedBy { it.first }.forEach { (revision, branch) ->
			val svnRevision = SVNRevision.create(revision)
			if (currentBranch != branch) {
				if (gitRepository.branchDoesNotExist(branch)) {
					println("Creating new branch: $branch")
					val originalRevision = repositoryInformation.branchCreationPoints[branch]!!.second
					gitRepository.branchCreate().setName(branch).setStartPoint(revisionCommits[originalRevision]!!.name).call()
				}
				println("Switching to branch: $branch")
				gitRepository.checkout().setName(branch).call()
				currentBranch = branch
			}
			clearWorkDirectory(workDirectory.toPath())
			val path = branchDefinitions[branch]!!.pathAt(revision)!!
			print("@${revision}: $path...")
			svnClientManager.updateClient.doExport(svnUrl.appendPath(path, false), workDirectory, svnRevision, svnRevision, "LF", true, SVNDepth.INFINITY)
			gitRepository.add().addFilepattern(".").setUpdate(false).call()
			gitRepository.add().addFilepattern(".").setUpdate(true).call()
			val logEntry = simpleSvn.getLogEntry(path, revision)!!
			val commit = gitRepository.commit()
				.setAuthor(PersonIdent(committers[logEntry.author], logEntry.date))
				.setMessage(logEntry.message)
				.setSign(false)
				.call()
			revisionCommits[revision] = commit
			println("\b\b\b -> $commit")
		}
	}
}

private fun Git.branchDoesNotExist(branch: String) =
	"refs/heads/$branch" !in branchList().call().map(Ref::getName)

private val xmlMapper = XmlMapper()

private fun clearWorkDirectory(path: Path) {
	Files.walkFileTree(path, object : FileVisitor<Path> {
		override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
			if (dir == path.resolve(".git")) {
				return FileVisitResult.SKIP_SUBTREE
			}
			return FileVisitResult.CONTINUE
		}

		override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
			file.toFile().delete()
			return FileVisitResult.CONTINUE
		}

		override fun visitFileFailed(file: Path, exc: IOException?): FileVisitResult {
			return FileVisitResult.TERMINATE
		}

		override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
			dir.toFile().delete()
			return FileVisitResult.CONTINUE
		}
	})
}
