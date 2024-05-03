package net.pterodactylus.utils.svn

import org.junit.jupiter.api.Test
import org.tmatesoft.svn.core.SVNURL
import java.io.File

class SimpleSVNTest {

	@Test
	fun `SimpleSVN can be created`() {
		SimpleSVN(SVNURL.fromFile(File(".")))
	}

}
