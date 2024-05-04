package net.pterodactylus.utils.svn

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test

class BranchDefinitionTest {

	@Test
	fun `branch information can find path for revision`() {
		val branchDefinition = BranchDefinition(3L to "/main", 12L to "/trunk/main", 45L to "/trunk/other/main")
		assertThat(branchDefinition.pathAt(7), equalTo("/main"))
		assertThat(branchDefinition.pathAt(17), equalTo("/trunk/main"))
		assertThat(branchDefinition.pathAt(47), equalTo("/trunk/other/main"))
	}

	@Test
	fun `branch definition returns null for path before first revision`() {
		val branchDefinition = BranchDefinition(3L to "/main", 12L to "/trunk/main", 45L to "/trunk/other/main")
		assertThat(branchDefinition.pathAt(2), nullValue())
	}

}
