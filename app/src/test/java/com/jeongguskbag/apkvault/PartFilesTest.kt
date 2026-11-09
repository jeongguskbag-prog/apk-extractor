package com.jeongguskbag.apkvault

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Random
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PartFilesTest {

    @get:Rule val tmp = TemporaryFolder()

    /** 실제 XAPK 처럼 여러 항목이 든 ZIP (압축 안 되는 무작위 데이터라 크기가 커짐) */
    private fun makeZip(seed: Long, entries: Int = 4, entrySize: Int = 150_000): ByteArray {
        val rnd = Random(seed)
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            for (i in 0 until entries) {
                z.putNextEntry(ZipEntry("part$i.apk"))
                z.write(ByteArray(entrySize).also { rnd.nextBytes(it) })
                z.closeEntry()
            }
            z.putNextEntry(ZipEntry("manifest.json"))
            z.write("""{"package_name":"x"}""".toByteArray())
            z.closeEntry()
        }
        return bos.toByteArray()
    }

    /** 앱의 분할 저장과 같은 방식: 앞에서부터 size 바이트씩 */
    private fun split(data: ByteArray, size: Int): List<ByteArray> =
        (data.indices step size).map { data.copyOfRange(it, minOf(it + size, data.size)) }

    private fun fileOf(parts: List<ByteArray>): File {
        val f = tmp.newFile()
        f.outputStream().use { out -> parts.forEach { out.write(it) } }
        return f
    }

    @Test fun parsesPartNames() {
        assertEquals("카톡_10.8.xapk" to 2, PartFiles.parse("카톡_10.8.xapk.002"))
        assertEquals("a.apk" to 11, PartFiles.parse("a.apk.011"))
        assertNull(PartFiles.parse("a.apk"))
        assertNull(PartFiles.parse("a.apk.1"))
        assertNull(PartFiles.parse(".001"))
    }

    @Test fun plansInNumberOrder() {
        val plan = PartFiles.plan(listOf("c" to "x.xapk.003", "a" to "x.xapk.001", "b" to "x.xapk.002"))
        assertEquals("x.xapk", plan.base)
        assertEquals(listOf("a", "b", "c"), plan.ordered)
    }

    private fun expectProblem(code: PartFiles.Code, items: List<Pair<String, String>>) {
        try {
            PartFiles.plan(items)
            fail("expected $code")
        } catch (p: PartFiles.Problem) {
            assertEquals(code, p.code)
        }
    }

    @Test fun rejectsBadSelections() {
        expectProblem(PartFiles.Code.NOT_PART, listOf("a" to "x.xapk.001", "b" to "photo.jpg"))
        expectProblem(PartFiles.Code.MIXED, listOf("a" to "x.xapk.001", "b" to "y.xapk.002"))
        expectProblem(PartFiles.Code.MISSING, listOf("a" to "x.xapk.001", "c" to "x.xapk.003"))
        expectProblem(PartFiles.Code.MISSING, listOf("b" to "x.xapk.002"))
        expectProblem(PartFiles.Code.DUPLICATE, listOf("a" to "x.xapk.001", "b" to "x.xapk.001"))
    }

    @Test fun splitThenJoinRestoresTheSameFile() {
        val zip = makeZip(1)
        val parts = split(zip, 100_000)
        val named = parts.mapIndexed { i, p -> p to "app.xapk.%03d".format(i + 1) }.shuffled(Random(7))
        val plan = PartFiles.plan(named)
        val joined = fileOf(plan.ordered)
        assertArrayEquals(zip, joined.readBytes())
        assertEquals(PartFiles.ZipCheck.OK, PartFiles.checkZip(joined))
    }

    @Test fun detectsMissingSwappedForeignAndExtraParts() {
        val parts = split(makeZip(2), 100_000)
        assert(parts.size >= 5)
        // 마지막 조각 빠짐
        assertEquals(PartFiles.ZipCheck.INCOMPLETE, PartFiles.checkZip(fileOf(parts.dropLast(1))))
        // 가운데 조각 빠짐
        assertEquals(PartFiles.ZipCheck.INCOMPLETE, PartFiles.checkZip(fileOf(parts.filterIndexed { i, _ -> i != 2 })))
        // 순서 바뀜
        val swapped = parts.toMutableList().also { val t = it[1]; it[1] = it[2]; it[2] = t }
        assertNotEquals(PartFiles.ZipCheck.OK, PartFiles.checkZip(fileOf(swapped)))
        // 다른 파일의 조각이 섞임 (같은 크기)
        val other = split(makeZip(3), 100_000)
        val mixed = parts.toMutableList().also { it[1] = other[1] }
        assertNotEquals(PartFiles.ZipCheck.OK, PartFiles.checkZip(fileOf(mixed)))
        // 조각이 한 번 더 들어감
        assertEquals(PartFiles.ZipCheck.INCOMPLETE, PartFiles.checkZip(fileOf(parts + listOf(parts.last()))))
        // 첫 조각이 아닌 것으로 시작
        assertEquals(PartFiles.ZipCheck.NOT_ZIP, PartFiles.checkZip(fileOf(parts.drop(1))))
    }

    @Test fun findsSiblingsInAFolder() {
        val folder = listOf(
            "u3" to "app.xapk.003", "u1" to "app.xapk.001", "x" to "other.xapk.001",
            "p" to "photo.jpg", "u2" to "app.xapk.002", "y" to "app.xapk",
        )
        val found = PartFiles.siblings("app.xapk", folder)!!
        assertEquals(listOf("u1", "u2", "u3"), found.map { it.first })
        // 가운데가 없으면 null
        assertNull(PartFiles.siblings("app.xapk", folder.filter { it.first != "u2" }))
        // 하나도 없으면 null
        assertNull(PartFiles.siblings("none.apk", folder))
        // 밑줄·점이 든 이름도 정확히 같은 이름만 (비슷한 이름 섞이지 않음)
        val tricky = listOf("a" to "카톡_10.8.xapk.001", "b" to "카톡_10.8.xapk.002", "c" to "카톡X10.8.xapk.002")
        assertEquals(listOf("a", "b"), PartFiles.siblings("카톡_10.8.xapk", tricky)!!.map { it.first })
    }

    @Test fun zipNames() {
        assert(PartFiles.isZipName("a.xapk"))
        assert(PartFiles.isZipName("A.APK"))
        assert(!PartFiles.isZipName("movie.mp4"))
    }
}
