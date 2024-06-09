package net.pterodactylus.sactilgis

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.greaterThan
import org.junit.jupiter.api.Test
import java.util.TreeSet

class WorklistTest {

	@Test
	fun `can create worklist`() {
		Worklist()
	}

	@Test
	fun `worklist for single branch contains all revisions of branch in order`() {
		val worklist = Worklist(mapOf("main" to treeSetOf(1L, 2L, 3L)))
		assertThat(worklist.createPlan(), contains("main" to 1L, "main" to 2L, "main" to 3L))
	}

	@Test
	fun `worklist for branch created from other branch lists first branch first, then second`() {
		val worklist = Worklist(
			mapOf(
				"main" to treeSetOf(1L, 2L, 3L, 4L, 8L, 9L),
				"second" to treeSetOf(5L, 6L, 7L)
			),
			mapOf("second" to ("main" to 3L))
		)
		assertThat(
			worklist.createPlan(), contains(
				"main" to 1L,
				"main" to 2L,
				"main" to 3L,
				"main" to 4L,
				"main" to 8L,
				"main" to 9L,
				"second" to 5L,
				"second" to 6L,
				"second" to 7L,
			)
		)
	}

	@Test
	fun `worklist for two merged branches lists first branch, second branch, first branch again`() {
		val worklist = Worklist(
			mapOf(
				"main" to treeSetOf(1L, 2L, 3L, 4L, 6L, 8L, 9L),
				"second" to treeSetOf(5L, 7L)
			),
			mapOf("second" to ("main" to 3L)),
			mapOf("main" to mapOf(8L to ("second" to 7L)))
		)
		assertThat(
			worklist.createPlan(), contains(
				"main" to 1L,
				"main" to 2L,
				"main" to 3L,
				"main" to 4L,
				"main" to 6L,
				"second" to 5L,
				"second" to 7L,
				"main" to 8L,
				"main" to 9L,
			)
		)
	}

	@Test
	fun `worklist for mutliple back-and-forth merges lists branches in correct order`() {
		val worklist = Worklist(
			mapOf(
				"main" to treeSetOf(1L, 2L, 3L, 4L, 6L, 8L, 9L, 13L, 14L),
				"second" to treeSetOf(5L, 7L, 10L, 11L, 12L, 15L, 16L)
			),
			mapOf("second" to ("main" to 3L)),
			mapOf(
				"main" to mapOf(8L to ("second" to 7L), 13L to ("second" to 12L)),
				"second" to mapOf(12L to ("main" to 11L))
			)
		)
		assertThat(
			worklist.createPlan(), contains(
				"main" to 1L,
				"main" to 2L,
				"main" to 3L,
				"main" to 4L,
				"main" to 6L,
				"second" to 5L,
				"second" to 7L,
				"main" to 8L,
				"main" to 9L,
				"second" to 10L,
				"second" to 11L,
				"second" to 12L,
				"main" to 13L,
				"main" to 14L,
				"second" to 15L,
				"second" to 16L,
			)
		)
	}

	@Test
	fun `worklist for first merge on-non main branch lists branches in correct order`() {
		val worklist = Worklist(
			mapOf(
				"main" to treeSetOf(1L, 2L, 3L, 4L),
				"second" to treeSetOf(5L, 6L),
			),
			mapOf("second" to ("main" to 1L)),
			mapOf(
				"second" to mapOf(6L to ("main" to 4L)),
			)
		)
		val plan = worklist.createPlan()
		assertThat(
			plan, contains(
				"main" to 1L,
				"main" to 2L,
				"main" to 3L,
				"main" to 4L,
				"second" to 5L,
				"second" to 6L,
			)
		)
	}

	@Test
	fun `worklist for merges with three branches lists branches in correct order`() {
		val worklist = Worklist(
			mapOf(
				"main" to treeSetOf(1L, 2L, 3L, 4L, 9L, 13L, 14L, 17L, 18L),
				"second" to treeSetOf(5L, 6L, 10L, 15L, 16L),
				"third" to treeSetOf(7L, 8L, 11L, 12L)
			),
			mapOf("second" to ("main" to 1L), "third" to ("main" to 2L)),
			mapOf(
				"main" to mapOf(9L to ("third" to 8L), 14L to ("second" to 13L), 17L to ("third" to 16L), 18L to ("second" to 17L)),
				"second" to mapOf(10L to ("main" to 9L), 15L to ("third" to 14L), 16L to ("main" to 15L)),
				"third" to mapOf(8L to ("main" to 4L), 12L to ("second" to 11L))
			)
		)
		val plan = worklist.createPlan()
		assertThat(
			plan, contains(
				"main" to 1L,
				"main" to 2L,
				"main" to 3L,
				"main" to 4L,
				"third" to 7L,
				"third" to 8L,
				"main" to 9L,
				"main" to 13L,
				"second" to 5L,
				"second" to 6L,
				"second" to 10L,
				"main" to 14L,
				"third" to 11L,
				"third" to 12L,
				"main" to 17L,
				"second" to 15L,
				"second" to 16L,
				"main" to 18L,
			)
		)
	}

	@Test
	fun `worklist lists orphan branches with branch creation points after their origin branches`() {
		val worklist = Worklist(
			mapOf("main" to treeSetOf(1L, 2L, 3L), "third" to treeSetOf(4L, 6L, 8L), "first" to treeSetOf(5L, 9L, 10L), "second" to treeSetOf(7L, 11L)),
			mapOf("first" to ("main" to 2L)),
			mapOf(),
		)
		val plan = worklist.createPlan()
		assertThat(plan.indexOf("first" to 5L), greaterThan(plan.indexOf("main" to 2L)))
	}

	@Test
	fun `worklist for revisions existing on multiple branches lists revisions multiple times`() {
		val worklist = Worklist(
			mapOf("main" to treeSetOf(1L, 2L, 3L), "second" to treeSetOf(2L, 4L)),
			mapOf(),
			mapOf()
		)
		val plan = worklist.createPlan()
		assertThat(plan, contains(
			"main" to 1L,
			"main" to 2L,
			"main" to 3L,
			"second" to 2L,
			"second" to 4L
		))
	}

}

private inline fun <reified T> treeSetOf(vararg values: T): TreeSet<T> = TreeSet<T>(setOf(*values))
