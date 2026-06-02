#!/usr/bin/env kotlin

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
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

class CalculatorEngine {
    var memory = 0.0
        private set
    var lastAnswer = 0.0
        private set
    var angleMode = AngleMode.DEGREES

    fun evaluate(expression: String, variables: Map<String, Double> = emptyMap()): Double {
        val result = ExpressionParser(expression, lastAnswer, memory, angleMode, variables).parse()
        lastAnswer = result
        return result
    }

    fun clearAnswer() {
        lastAnswer = 0.0
    }

    fun clearMemory() {
        memory = 0.0
    }

    fun recallMemory(): Double = memory

    fun addMemory(value: Double = lastAnswer) {
        memory += value
    }

    fun subtractMemory(value: Double = lastAnswer) {
        memory -= value
    }

    fun storeMemory(value: Double = lastAnswer) {
        memory = value
    }

    fun derivative(expression: String, atX: Double): Double {
        val result = Calculus.derivative(expression, atX, this)
        lastAnswer = result
        return result
    }

    fun integral(expression: String, from: Double, to: Double): Double {
        val result = Calculus.integral(expression, from, to, this)
        lastAnswer = result
        return result
    }
}

class CalculatorCli(private val engine: CalculatorEngine = CalculatorEngine()) {
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
                        engine.clearAnswer()
                        println("Cleared. Memory is still ${format(engine.memory)}.")
                    }
                    "mc" -> {
                        engine.clearMemory()
                        println("Memory cleared.")
                    }
                    "mr" -> println(format(engine.recallMemory()))
                    "deg" -> {
                        engine.angleMode = AngleMode.DEGREES
                        println("Angle mode: degrees")
                    }
                    "rad" -> {
                        engine.angleMode = AngleMode.RADIANS
                        println("Angle mode: radians")
                    }
                    else -> handleInput(line)
                }
            } catch (error: IllegalArgumentException) {
                println("Error: ${error.message}")
            } catch (error: ArithmeticException) {
                println("Math error: ${error.message}")
            }
        }
    }

    private fun handleInput(line: String) {
        val calculusResult = tryCalculusCommand(line)
        if (calculusResult != null) {
            println(format(calculusResult))
            return
        }

        val lower = line.lowercase()
        val memoryCommand = listOf("m+", "m-", "ms").firstOrNull { lower == it || lower.startsWith("$it ") }

        if (memoryCommand != null) {
            val expression = line.drop(memoryCommand.length).trim()
            val value = if (expression.isEmpty()) engine.lastAnswer else engine.evaluate(expression)

            when (memoryCommand) {
                "m+" -> engine.addMemory(value)
                "m-" -> engine.subtractMemory(value)
                "ms" -> engine.storeMemory(value)
            }

            println("Memory = ${format(engine.memory)}")
            return
        }

        println(format(engine.evaluate(line)))
    }

    private fun tryCalculusCommand(line: String): Double? {
        val derivativeMatch = Regex("^(derivative|derive|d)\\s+(.+)\\s+at\\s+(.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(line)
        if (derivativeMatch != null) {
            val expression = derivativeMatch.groupValues[2]
            val atX = engine.evaluate(derivativeMatch.groupValues[3])
            return engine.derivative(expression, atX)
        }

        val integralMatch = Regex("^(integral|integrate)\\s+(.+)\\s+from\\s+(.+)\\s+to\\s+(.+)$", RegexOption.IGNORE_CASE)
            .matchEntire(line)
        if (integralMatch != null) {
            val expression = integralMatch.groupValues[2]
            val from = engine.evaluate(integralMatch.groupValues[3])
            val to = engine.evaluate(integralMatch.groupValues[4])
            return engine.integral(expression, from, to)
        }

        return null
    }

    private fun printWelcome() {
        println("Kotlin Calculator")
        println("Type an expression, help, deg, rad, mc, mr, m+, m-, ms, clear, or quit.")
        println("Examples: 12 + 7 * 3, sqrt(81), sin(30), 50 + 10%, 2^8")
        println("Calculus: derivative x^2 + 3*x at 2, integral x^2 from 0 to 3")
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
              pi, e, ans, mem, x for calculus expressions

            Calculus:
              derivative x^2 + 3*x at 2
              integral x^2 from 0 to 3

            Memory and controls:
              m+ [expr] adds to memory       m- [expr] subtracts from memory
              ms [expr] stores memory        mr recalls memory
              mc clears memory               clear resets the current answer
              deg/rad changes trig mode      quit exits
            """.trimIndent()
        )
    }
}

class CalculatorFrame(private val engine: CalculatorEngine = CalculatorEngine()) : JFrame("Kotlin Calculator") {
    private val display = JTextField("0")
    private val history = JTextArea()
    private val angleToggle = JToggleButton("DEG")
    private val calculusExpression = JTextField("x^2 + 3*x")
    private val xValue = JTextField("2")
    private val fromValue = JTextField("0")
    private val toValue = JTextField("3")
    private val calculusResult = JTextField()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(520, 720)
        contentPane.background = Color(245, 247, 250)
        layout = BorderLayout(10, 10)

        add(buildTopPanel(), BorderLayout.NORTH)
        add(buildButtonPanel(), BorderLayout.CENTER)
        add(buildBottomPanel(), BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(null)
    }

    private fun buildTopPanel(): JPanel {
        val panel = JPanel(BorderLayout(8, 8))
        panel.border = BorderFactory.createEmptyBorder(14, 14, 0, 14)
        panel.background = contentPane.background

        display.horizontalAlignment = SwingConstants.RIGHT
        display.font = Font("SansSerif", Font.PLAIN, 30)
        display.border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
        panel.add(display, BorderLayout.NORTH)

        history.isEditable = false
        history.font = Font("Monospaced", Font.PLAIN, 13)
        history.rows = 5
        panel.add(JScrollPane(history), BorderLayout.CENTER)

        return panel
    }

    private fun buildButtonPanel(): JPanel {
        val buttons = arrayOf(
            arrayOf("MC", "MR", "M+", "M-", "MS"),
            arrayOf("AC", "(", ")", "%", "/"),
            arrayOf("7", "8", "9", "*", "^"),
            arrayOf("4", "5", "6", "-", "sqrt("),
            arrayOf("1", "2", "3", "+", "sin("),
            arrayOf("0", ".", "pi", "e", "="),
            arrayOf("cos(", "tan(", "ln(", "log(", "DEL")
        )

        val panel = JPanel(GridLayout(buttons.size, buttons[0].size, 8, 8))
        panel.border = BorderFactory.createEmptyBorder(10, 14, 10, 14)
        panel.background = contentPane.background

        for (row in buttons) {
            for (label in row) {
                panel.add(makeButton(label))
            }
        }

        return panel
    }

    private fun buildBottomPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        panel.border = BorderFactory.createEmptyBorder(0, 14, 14, 14)
        panel.background = contentPane.background
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 1.0
        constraints.insets.set(4, 4, 4, 4)

        angleToggle.addActionListener {
            if (angleToggle.isSelected) {
                engine.angleMode = AngleMode.RADIANS
                angleToggle.text = "RAD"
            } else {
                engine.angleMode = AngleMode.DEGREES
                angleToggle.text = "DEG"
            }
        }

        calculusResult.isEditable = false
        calculusResult.horizontalAlignment = SwingConstants.RIGHT

        addToGrid(panel, JLabel("f(x)"), constraints, 0, 0, 1)
        addToGrid(panel, calculusExpression, constraints, 1, 0, 3)
        addToGrid(panel, angleToggle, constraints, 4, 0, 1)

        addToGrid(panel, JLabel("x"), constraints, 0, 1, 1)
        addToGrid(panel, xValue, constraints, 1, 1, 1)
        addToGrid(panel, makeActionButton("d/dx") { runDerivative() }, constraints, 2, 1, 1)
        addToGrid(panel, JLabel("Result"), constraints, 3, 1, 1)
        addToGrid(panel, calculusResult, constraints, 4, 1, 1)

        addToGrid(panel, JLabel("from"), constraints, 0, 2, 1)
        addToGrid(panel, fromValue, constraints, 1, 2, 1)
        addToGrid(panel, JLabel("to"), constraints, 2, 2, 1)
        addToGrid(panel, toValue, constraints, 3, 2, 1)
        addToGrid(panel, makeActionButton("integral") { runIntegral() }, constraints, 4, 2, 1)

        return panel
    }

    private fun addToGrid(panel: JPanel, component: java.awt.Component, constraints: GridBagConstraints, x: Int, y: Int, width: Int) {
        constraints.gridx = x
        constraints.gridy = y
        constraints.gridwidth = width
        panel.add(component, constraints)
    }

    private fun makeButton(label: String): JButton {
        return makeActionButton(label) {
            when (label) {
                "=" -> evaluateDisplay()
                "AC" -> {
                    display.text = "0"
                    engine.clearAnswer()
                }
                "DEL" -> {
                    display.text = display.text.dropLast(1).ifBlank { "0" }
                }
                "MC" -> {
                    engine.clearMemory()
                    appendHistory("Memory cleared")
                }
                "MR" -> setDisplay(format(engine.recallMemory()))
                "M+" -> {
                    engine.addMemory(readDisplayValue())
                    appendHistory("M+ -> ${format(engine.memory)}")
                }
                "M-" -> {
                    engine.subtractMemory(readDisplayValue())
                    appendHistory("M- -> ${format(engine.memory)}")
                }
                "MS" -> {
                    engine.storeMemory(readDisplayValue())
                    appendHistory("MS -> ${format(engine.memory)}")
                }
                else -> appendToDisplay(label)
            }
        }
    }

    private fun makeActionButton(label: String, action: () -> Unit): JButton {
        val button = JButton(label)
        button.font = Font("SansSerif", Font.BOLD, 16)
        button.isFocusPainted = false
        button.addActionListener {
            try {
                action()
            } catch (error: IllegalArgumentException) {
                showError(error.message ?: "Invalid input.")
            } catch (error: ArithmeticException) {
                showError(error.message ?: "Math error.")
            }
        }
        return button
    }

    private fun evaluateDisplay() {
        val expression = display.text
        val result = engine.evaluate(expression)
        setDisplay(format(result))
        appendHistory("$expression = ${format(result)}")
    }

    private fun runDerivative() {
        val expression = calculusExpression.text.trim()
        val atX = ExpressionParser(xValue.text, engine.lastAnswer, engine.memory, engine.angleMode).parse()
        val result = engine.derivative(expression, atX)
        calculusResult.text = format(result)
        appendHistory("d/dx $expression at x=${format(atX)} = ${format(result)}")
    }

    private fun runIntegral() {
        val expression = calculusExpression.text.trim()
        val from = ExpressionParser(fromValue.text, engine.lastAnswer, engine.memory, engine.angleMode).parse()
        val to = ExpressionParser(toValue.text, engine.lastAnswer, engine.memory, engine.angleMode).parse()
        val result = engine.integral(expression, from, to)
        calculusResult.text = format(result)
        appendHistory("integral $expression from ${format(from)} to ${format(to)} = ${format(result)}")
    }

    private fun readDisplayValue(): Double = engine.evaluate(display.text)

    private fun appendToDisplay(text: String) {
        display.text = if (display.text == "0") text else display.text + text
    }

    private fun setDisplay(text: String) {
        display.text = text
    }

    private fun appendHistory(text: String) {
        history.append("$text\n")
        history.caretPosition = history.document.length
    }

    private fun showError(message: String) {
        display.text = "Error"
        appendHistory("Error: $message")
    }
}

object Calculus {
    fun derivative(expression: String, atX: Double, engine: CalculatorEngine): Double {
        val step = maxOf(0.00001, abs(atX) * 0.00001)
        val left = evaluateAt(expression, atX - step, engine)
        val right = evaluateAt(expression, atX + step, engine)
        return (right - left) / (2.0 * step)
    }

    fun integral(expression: String, from: Double, to: Double, engine: CalculatorEngine): Double {
        require(from.isFinite() && to.isFinite()) { "Integral bounds must be finite numbers." }
        if (from == to) return 0.0

        val slices = 1000
        val width = (to - from) / slices
        var sum = evaluateAt(expression, from, engine) + evaluateAt(expression, to, engine)

        for (index in 1 until slices) {
            val x = from + index * width
            sum += if (index % 2 == 0) 2.0 * evaluateAt(expression, x, engine) else 4.0 * evaluateAt(expression, x, engine)
        }

        return sum * width / 3.0
    }

    private fun evaluateAt(expression: String, x: Double, engine: CalculatorEngine): Double {
        return ExpressionParser(expression, engine.lastAnswer, engine.memory, engine.angleMode, mapOf("x" to x)).parse()
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
    private val angleMode: AngleMode,
    private val variables: Map<String, Double> = emptyMap()
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

            return when {
                variables.containsKey(name) -> variables.getValue(name)
                name == "pi" -> PI
                name == "e" -> E
                name == "ans" -> lastAnswer
                name == "mem" || name == "memory" -> memory
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

fun format(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return value.toString()

    val rounded = value.roundToLong()
    if (abs(value - rounded) < 0.0000000001) return rounded.toString()

    return "%,.10f".format(value)
        .trimEnd('0')
        .trimEnd('.')
}

if (args.any { it == "--cli" }) {
    CalculatorCli().start()
} else {
    System.setProperty("java.awt.headless", "false")
    SwingUtilities.invokeLater {
        try {
            CalculatorFrame().isVisible = true
        } catch (error: Throwable) {
            println("Could not open the graphical calculator window.")
            println("Run from a normal Windows desktop terminal, not WSL, SSH, Docker, or a headless server.")
            println("Error: ${error.message}")
        }
    }
}
