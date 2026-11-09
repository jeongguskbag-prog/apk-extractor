package com.jeongguskbag.apkvault

import java.io.File
import java.io.RandomAccessFile

/**
 * 조각 파일(이름.확장자.001, .002 …) 합치기에 필요한 판단을 모은 곳.
 * 안드로이드 API 를 쓰지 않아 휴대폰 없이 단위 테스트(src/test)로 검사할 수 있다.
 */
object PartFiles {

    private val PART = Regex("""^(.+)\.(\d{3})$""")

    /** 합치기 전에 찾은 문제. code 는 화면 문구를 고르는 데 씀 */
    class Problem(val code: Code, val detail: String) : Exception("$code: $detail")

    enum class Code { NOT_PART, MIXED, DUPLICATE, MISSING, INCOMPLETE }

    /** 합칠 순서가 정해진 결과: 원래 파일 이름(base)과 001, 002 … 순서의 항목들 */
    class Plan<T>(val base: String, val ordered: List<T>)

    /** "카톡.xapk.002" → ("카톡.xapk", 2). 조각 이름이 아니면 null */
    fun parse(name: String): Pair<String, Int>? {
        val m = PART.matchEntire(name.trim()) ?: return null
        return m.groupValues[1] to m.groupValues[2].toInt()
    }

    /**
     * 고른 파일들(항목, 이름)을 번호순으로 정렬하고, 같은 파일의 조각들이며 001 부터 빠짐없는지 확인.
     * 문제가 있으면 Problem 을 던짐
     */
    fun <T> plan(items: List<Pair<T, String>>): Plan<T> {
        require(items.isNotEmpty())
        val parsed = items.map { (item, name) ->
            val p = parse(name) ?: throw Problem(Code.NOT_PART, name)
            Triple(item, p.first, p.second)
        }
        val bases = parsed.map { it.second }.distinct()
        if (bases.size > 1) throw Problem(Code.MIXED, bases.joinToString(", "))
        val numbers = parsed.map { it.third }
        val dup = numbers.groupBy { it }.filter { it.value.size > 1 }.keys
        if (dup.isNotEmpty()) throw Problem(Code.DUPLICATE, dup.sorted().joinToString(", ") { String.format(java.util.Locale.ROOT, "%03d", it) })
        val max = numbers.maxOrNull()!!
        val missing = (1..max).filter { it !in numbers }
        if (missing.isNotEmpty()) throw Problem(Code.MISSING, missing.joinToString(", ") { String.format(java.util.Locale.ROOT, "%03d", it) })
        return Plan(bases.first(), parsed.sortedBy { it.third }.map { it.first })
    }

    /**
     * 폴더 안 파일들(항목, 이름) 중 base 의 조각만 골라 번호순으로. 조각 하나만 골랐을 때 나머지를 찾는 데 씀.
     * 001 부터 빠짐없이 이어지지 않으면 null (빠진 조각이 있음)
     */
    fun <T> siblings(base: String, candidates: List<Pair<T, String>>): List<Pair<T, String>>? {
        val mine = candidates.mapNotNull { (item, name) ->
            val p = parse(name) ?: return@mapNotNull null
            if (p.first == base) Triple(item, name, p.second) else null
        }.distinctBy { it.third }.sortedBy { it.third }
        if (mine.isEmpty()) return null
        if (mine.map { it.third } != (1..mine.size).toList()) return null
        return mine.map { it.first to it.second }
    }

    /** 한 파일의 조각 묶음: 첫 조각 이름(…001), 번호순 항목, 001 부터 빠짐없는지 */
    class Group<T>(val base: String, val firstName: String, val parts: List<T>, val complete: Boolean, val missing: List<Int>)

    /** 폴더 목록(항목, 이름)에서 조각 파일만 골라 파일별로 묶음. 001 이 있는 묶음만, 이름순 */
    fun <T> groups(items: List<Pair<T, String>>): List<Group<T>> =
        items.mapNotNull { (item, name) -> parse(name)?.let { Triple(item, it.first, it.second) } }
            .groupBy { it.second }
            .mapNotNull { (base, list) ->
                val byNumber = list.distinctBy { it.third }.sortedBy { it.third }
                if (byNumber.first().third != 1) return@mapNotNull null // 001 이 없으면 목록에 안 보임
                val numbers = byNumber.map { it.third }
                val missing = (1..numbers.last()).filter { it !in numbers }
                Group(base, String.format(java.util.Locale.ROOT, "%s.%03d", base, 1), byNumber.map { it.first }, missing.isEmpty(), missing)
            }
            .sortedBy { it.base.lowercase() }

    /**
     * OK: 완전함 / NOT_ZIP: ZIP 이 아님(첫 조각이 아닌 걸로 시작 등) / INCOMPLETE: 구조가 어긋남(조각 빠짐·더해짐·순서) /
     * CORRUPT: 구조는 맞지만 내용 CRC 오류(다른 파일 조각이 섞임 등) / UNKNOWN: 4GB 넘는 ZIP64 라 검사 못 함
     */
    enum class ZipCheck { OK, NOT_ZIP, INCOMPLETE, CORRUPT, UNKNOWN }

    /** APK·XAPK 처럼 ZIP 인 확장자인지 (합친 뒤 완전한지 검사할 수 있음) */
    fun isZipName(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in setOf("apk", "xapk", "apks", "apkm", "zip")

    private fun u32(b: ByteArray, o: Int): Long =
        (b[o].toLong() and 0xff) or ((b[o + 1].toLong() and 0xff) shl 8) or
            ((b[o + 2].toLong() and 0xff) shl 16) or ((b[o + 3].toLong() and 0xff) shl 24)

    /**
     * 합친 ZIP 이 완전한지 검사 (두 단계).
     * 1) 구조: ZIP 맨 끝 "목차 끝" 기록의 목차 위치·크기가 실제 위치와 정확히 맞아야 함 →
     *    조각이 빠지거나, 더해지거나, 순서가 바뀌면 어긋남.
     * 2) 내용: 모든 항목을 읽어 CRC(오류 검출 값)를 확인 → 같은 크기의 다른 파일 조각이 섞인 것도 잡음.
     * 4GB 넘는 ZIP64 는 구조 검사를 못 해 UNKNOWN.
     */
    fun checkZip(file: File): ZipCheck {
        val structure = checkStructure(file)
        if (structure != ZipCheck.OK) return structure
        return try {
            java.util.zip.ZipInputStream(file.inputStream().buffered()).use { zin ->
                val buf = ByteArray(64 * 1024)
                var entries = 0
                while (true) {
                    zin.nextEntry ?: break
                    while (zin.read(buf) >= 0) { /* 끝까지 읽어야 CRC 를 확인함 */ }
                    entries++
                }
                if (entries == 0) ZipCheck.CORRUPT else ZipCheck.OK
            }
        } catch (e: java.io.IOException) {
            ZipCheck.CORRUPT // CRC 불일치 등 (ZipException 포함)
        }
    }

    private fun checkStructure(file: File): ZipCheck {
        val len = file.length()
        if (len < 22) return ZipCheck.NOT_ZIP
        RandomAccessFile(file, "r").use { raf ->
            val head = ByteArray(4)
            raf.readFully(head)
            if (u32(head, 0) != 0x04034b50L) return ZipCheck.NOT_ZIP
            val tailLen = minOf(len, 22L + 65535L).toInt()
            val tail = ByteArray(tailLen)
            raf.seek(len - tailLen)
            raf.readFully(tail)
            var e = -1
            for (i in tailLen - 22 downTo 0) {
                if (u32(tail, i) == 0x06054b50L) { e = i; break }
            }
            if (e < 0) return ZipCheck.INCOMPLETE
            val eocdPos = len - tailLen + e
            val cdSize = u32(tail, e + 12)
            val cdOffset = u32(tail, e + 16)
            if (cdSize == 0xffffffffL || cdOffset == 0xffffffffL) return ZipCheck.UNKNOWN
            if (e >= 20 && u32(tail, e - 20) == 0x07064b50L) return ZipCheck.UNKNOWN // ZIP64
            if (cdOffset + cdSize != eocdPos) return ZipCheck.INCOMPLETE
            val sig = ByteArray(4)
            raf.seek(cdOffset)
            raf.readFully(sig)
            return if (u32(sig, 0) == 0x02014b50L) ZipCheck.OK else ZipCheck.INCOMPLETE
        }
    }
}
