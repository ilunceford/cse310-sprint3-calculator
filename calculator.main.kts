#!/usr/bin/env kotlin

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class Calculator {
    private var memory = 0.0
    private var lastAnswer = 0.0
    private var angleMode = AngleMode.DEGREES

    fun start() {
        printWelcome()

        while (true) {
            print("> ")
            val line = readLine()?.trim() ?: break
            if (line.isBlank()) continue

            try {
                when (line.lowercase()) {
                    "quit", "exit", "q" -> {
                        println("Goodbye.")
                        return
                    }
                    "help", "h", "?" -> printHelp()
                    "clear", "c", "ac" -> {
                        lastAnswer = 0.0
                        println("Cleared. Memory is still ${format(memory)}.")
                    }
                    "mc" -> {
                        memory = 0.0
                        println("Memory cleared.")
                    }
                    "mr" -> println(format(memory))
                    "deg" -> {
                        angleMode = AngleMode.DEGREES
                        println("Angle mode: degrees")
                    }
                    "rad" -> {
                        angleMode = AngleMode.RADIANS
                        println("Angle mode: radians")
                    }
                    else -> handleExpressionOrMemory(line)
                }
            } catch (error: IllegalArgumentException) {
                println("Error: ${error.message}")
            } catch (error: ArithmeticException) {
                println("Math error: ${error.message}")
            }
        }
    }

    private fun handleExpressionOrMemory(line: String) {
        val lower = line.lowercase()
        val memoryCommand = listOf("m+", "m-", "ms").firstOrNull { lower == it || lower.startsWith("$it ") }

        if (memoryCommand != null) {
            val expression = line.drop(memoryCommand.length).trim()
            val value = if (expression.isEmpty()) lastAnswer else evaluate(expression)

            when (memoryCommand) {
                "m+" -> memory += value
                "m-" -> memory -= value
                "ms" -> memory = value
            }

            println("Memory = ${format(memory)}")
            return
        }

        lastAnswer = evaluate(line)
        println(format(lastAnswer))
    }

    private fun evaluate(expression: String): Double {
        return ExpressionParser(expression, lastAnswer, memory, angleMode).parse()
    }

    private fun printWelcome() {
        println("Kotlin Calculator")
        println("Type an expression, help, deg, rad, mc, mr, m+, m-, ms, clear, or quit.")
        println("Examples: 12 + 7 * 3, sqrt(81), sin(30), 50 + 10%, 2^8")
    }

    private fun printHelp() {
        println(
            """
            Operators:
              +   add                 -   subtract / negative
              *   multiply            /   divide
              %   percent/postfix     mod remainder
              ^   power               !   factorial
              ( ) group expressions

            Functions:
              sqrt(x), cbrt(x), sqr(x), inv(x), abs(x)
              sin(x), cos(x), tan(x), asin(x), acos(x), atan(x)
              ln(x), log(x), exp(x)

            Constants and values:
              pi, e, ans, mem

            Memory and controls:
              m+ [expr] adds to memory       m- [expr] subtracts from memory
              ms [expr] stores memory        mr recalls memory
              mc clears memory               clear resets the current answer
              deg/rad changes trig mode      quit exits
            """.trimIndent()
        )
    }

    private fun format(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return value.toString()

        val rounded = value.roundToLong()
        if (abs(value - rounded) < 0.0000000001) return rounded.toString()

        return "%,.10f".format(value)
            .trimEnd('0')
            .trimEnd('.')
    }
}

enum class AngleMode {
    DEGREES,
    RADIANS
}

class ExpressionParser(
    private val source: String,
    private val lastAnswer: Double,
    private val memory: Double,
    private val angleMode: AngleMode
) {
    private var index = 0

    fun parse(): Double {
        val value = parseAddition()
        skipSpaces()
        if (!isAtEnd()) {
            throw IllegalArgumentException("Unexpected '${peek()}' at position ${index + 1}.")
        }
        return value
    }

    private fun parseAddition(): Double {
        var value = parseMultiplication()

        while (true) {
            skipSpaces()
            value = when {
                match('+') -> value + parseMultiplication()
                match('-') -> value - parseMultiplication()
                else -> return value
            }
        }
    }

    private fun parseMultiplication(): Double {
        var value = parsePower()

        while (true) {
            skipSpaces()
            value = when {
                match('*') -> value * parsePower()
                match('/') -> {
                    val divisor = parsePower()
                    if (divisor == 0.0) throw ArithmeticException("Cannot divide by zero.")
                    value / divisor
                }
                matchWord("mod") -> {
                    val divisor = parsePower()
                    if (divisor == 0.0) throw ArithmeticException("Cannot divide by zero.")
                    value % divisor
                }
                else -> return value
            }
        }
    }

    private fun parsePower(): Double {
        val base = parseUnary()
        skipSpaces()
        return if (match('^')) base.pow(parsePower()) else base
    }

    private fun parseUnary(): Double {
        skipSpaces()
        return when {
            match('+') -> parseUnary()
            match('-') -> -parseUnary()
            else -> parsePostfix()
        }
    }

    private fun parsePostfix(): Double {
        var value = parsePrimary()

        while (true) {
            skipSpaces()
            value = when {
                match('%') -> value / 100.0
                match('!') -> factorial(value)
                else -> return value
            }
        }
    }

    private fun parsePrimary(): Double {
        skipSpaces()

        if (match('(')) {
            val value = parseAddition()
            skipSpaces()
            expect(')')
            return value
        }

        if (peek()?.isDigit() == true || peek() == '.') {
            return parseNumber()
        }

        if (peek()?.isLetter() == true) {
            val name = parseIdentifier().lowercase()
            skipSpaces()

            if (match('(')) {
                val argument = parseAddition()
                skipSpaces()
                expect(')')
                return callFunction(name, argument)
            }

            return when (name) {
                "pi" -> PI
                "e" -> E
                "ans" -> lastAnswer
                "mem", "memory" -> memory
                else -> throw IllegalArgumentException("Unknown value '$name'.")
            }
        }

        throw IllegalArgumentException("Expected a number, value, function, or group at position ${index + 1}.")
    }

    private fun parseNumber(): Double {
        val start = index
        var hasDecimal = false

        while (!isAtEnd()) {
            val char = source[index]
            when {
                char.isDigit() -> index++
                char == '.' && !hasDecimal -> {
                    hasDecimal = true
                    index++
                }
                else -> break
            }
        }

        return source.substring(start, index).toDoubleOrNull()
            ?: throw IllegalArgumentException("Invalid number at position ${start + 1}.")
    }

    private fun parseIdentifier(): String {
        val start = index
        while (!isAtEnd() && source[index].isLetter()) index++
        return source.substring(start, index)
    }

    private fun callFunction(name: String, argument: Double): Double {
        return when (name) {
            "sqrt" -> {
                require(argument >= 0.0) { "sqrt needs a non-negative number." }
                sqrt(argument)
            }
            "cbrt" -> if (argument < 0.0) -(-argument).pow(1.0 / 3.0) else argument.pow(1.0 / 3.0)
            "sqr", "square" -> argument * argument
            "inv", "reciprocal" -> {
                if (argument == 0.0) throw ArithmeticException("Cannot divide by zero.")
                1.0 / argument
            }
            "abs" -> abs(argument)
            "sin" -> sin(toRadiansIfNeeded(argument))
            "cos" -> cos(toRadiansIfNeeded(argument))
            "tan" -> tan(toRadiansIfNeeded(argument))
            "asin" -> fromRadiansIfNeeded(asin(argument))
            "acos" -> fromRadiansIfNeeded(acos(argument))
            "atan" -> fromRadiansIfNeeded(atan(argument))
            "ln" -> {
                require(argument > 0.0) { "ln needs a positive number." }
                ln(argument)
            }
            "log" -> {
                require(argument > 0.0) { "log needs a positive number." }
                log10(argument)
            }
            "exp" -> exp(argument)
            else -> throw IllegalArgumentException("Unknown function '$name'.")
        }
    }

    private fun factorial(value: Double): Double {
        require(value >= 0.0) { "factorial needs a non-negative whole number." }
        require(abs(value - value.toLong()) < 0.0000000001) { "factorial needs a whole number." }
        require(value <= 170.0) { "factorial is too large for this calculator." }

        var result = 1.0
        for (number in 2..value.toInt()) result *= number
        return result
    }

    private fun toRadiansIfNeeded(value: Double): Double {
        return if (angleMode == AngleMode.DEGREES) value * PI / 180.0 else value
    }

    private fun fromRadiansIfNeeded(value: Double): Double {
        return if (angleMode == AngleMode.DEGREES) value * 180.0 / PI else value
    }

    private fun match(expected: Char): Boolean {
        skipSpaces()
        if (peek() != expected) return false
        index++
        return true
    }

    private fun matchWord(word: String): Boolean {
        skipSpaces()
        if (!source.regionMatches(index, word, 0, word.length, ignoreCase = true)) return false

        val before = if (index == 0) null else source[index - 1]
        val after = source.getOrNull(index + word.length)
        if (before?.isLetter() == true || after?.isLetter() == true) return false

        index += word.length
        return true
    }

    private fun expect(expected: Char) {
        if (!match(expected)) {
            throw IllegalArgumentException("Expected '$expected' at position ${index + 1}.")
        }
    }

    private fun skipSpaces() {
        while (!isAtEnd() && source[index].isWhitespace()) index++
    }

    private fun peek(): Char? = source.getOrNull(index)

    private fun isAtEnd(): Boolean = index >= source.length
}

Calculator().start()
