package inc.kaizen.infra.podium

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CollectionFormatsTest : FunSpec({

    test("CSVParams joins with commas") {
        val csv = CollectionFormats.CSVParams("a", "b", "c")
        csv.toString() shouldBe "a,b,c"
    }

    test("CSVParams with list constructor") {
        val csv = CollectionFormats.CSVParams(listOf("x", "y"))
        csv.toString() shouldBe "x,y"
    }

    test("CSVParams with empty list") {
        val csv = CollectionFormats.CSVParams(emptyList())
        csv.toString() shouldBe ""
    }

    test("CSVParams with single element") {
        val csv = CollectionFormats.CSVParams("only")
        csv.toString() shouldBe "only"
    }

    test("SSVParams joins with spaces") {
        val ssv = CollectionFormats.SSVParams("a", "b", "c")
        ssv.toString() shouldBe "a b c"
    }

    test("SSVParams with list constructor") {
        val ssv = CollectionFormats.SSVParams(listOf("x", "y"))
        ssv.toString() shouldBe "x y"
    }

    test("TSVParams joins with tabs") {
        val tsv = CollectionFormats.TSVParams("a", "b", "c")
        tsv.toString() shouldBe "a\tb\tc"
    }

    test("TSVParams with list constructor") {
        val tsv = CollectionFormats.TSVParams(listOf("x", "y"))
        tsv.toString() shouldBe "x\ty"
    }

    test("PIPESParams joins with pipes") {
        val pipes = CollectionFormats.PIPESParams("a", "b", "c")
        pipes.toString() shouldBe "a|b|c"
    }

    test("PIPESParams with list constructor") {
        val pipes = CollectionFormats.PIPESParams(listOf("x", "y"))
        pipes.toString() shouldBe "x|y"
    }

    test("SPACEParams is an SSVParams with empty params") {
        val space = CollectionFormats.SPACEParams()
        space.toString() shouldBe ""
    }

    test("CSVParams no-arg constructor creates empty params") {
        val csv = CollectionFormats.CSVParams()
        csv.params shouldBe emptyList()
        csv.toString() shouldBe ""
    }

    test("SSVParams no-arg constructor creates empty params") {
        val ssv = CollectionFormats.SSVParams()
        ssv.params shouldBe emptyList()
        ssv.toString() shouldBe ""
    }
})

